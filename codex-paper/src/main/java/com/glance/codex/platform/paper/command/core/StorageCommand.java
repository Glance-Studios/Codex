package com.glance.codex.platform.paper.command.core;

import com.glance.codex.api.data.storage.CollectableStorage;
import com.glance.codex.platform.paper.command.engine.CommandHandler;
import com.glance.codex.platform.paper.persistence.config.CollectableStorageConfig;
import com.glance.codex.platform.paper.persistence.file.FlatFileImport;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/** Reports which store is in use, and re-runs the flat file import on demand. */
@Slf4j
@Singleton
@AutoService(CommandHandler.class)
public class StorageCommand implements CommandHandler {

    private final Plugin plugin;
    private final CollectableStorageConfig cfg;
    private final Provider<CollectableStorage> storage;

    @Inject
    public StorageCommand(
        @NotNull Plugin plugin,
        @NotNull CollectableStorageConfig cfg,
        @NotNull Provider<CollectableStorage> storage
    ) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.storage = storage;
    }

    @Command("collectables|journal storage")
    @Permission("collectables.admin")
    public void status(@NotNull CommandSender sender) {
        File directory = directory();
        FlatFileImport job = job(storage.get());

        sender.sendMessage("Backend: " + cfg.backend());
        sender.sendMessage("Flat file directory: " + directory.getPath()
                + (directory.isDirectory() ? "" : " (absent)"));
        sender.sendMessage(job.pending()
                ? "There are flat files waiting to be imported. Run /collectables storage import"
                : "Nothing is waiting to be imported");
    }

    /**
     * Reads the flat files again, ignoring the marker left by an earlier run.
     *
     * A player already in the database is still skipped rather than merged, so this is safe to run
     * on a live server when an earlier import left files behind.
     */
    @Command("collectables|journal storage import")
    @Permission("collectables.admin")
    public void runImport(@NotNull CommandSender sender) {
        if (cfg.backend() == CollectableStorageConfig.Backend.FLATFILE) {
            sender.sendMessage("The backend is FLATFILE, so there is nowhere to import to. "
                    + "Point storage.backend at a database first.");
            return;
        }

        CollectableStorage target = storage.get();
        sender.sendMessage("Importing flat files into " + cfg.backend() + "...");

        CompletableFuture
            .supplyAsync(() -> job(target).run())
            .thenAccept(result -> sender.sendMessage(
                    "Imported " + result.imported() + ", skipped " + result.skipped()
                            + ", failed " + result.failed().size()
                            + ", of " + result.total() + " file(s)"))
            .exceptionally(ex -> {
                sender.sendMessage("Import failed: " + ex.getMessage());
                log.error("Failed to import the collectables flat files due to: ", ex);
                return null;
            });
    }

    private FlatFileImport job(@NotNull CollectableStorage target) {
        return new FlatFileImport(directory(), target, plugin.getLogger());
    }

    private File directory() {
        return new File(plugin.getDataFolder(), cfg.flatFileDir());
    }
}
