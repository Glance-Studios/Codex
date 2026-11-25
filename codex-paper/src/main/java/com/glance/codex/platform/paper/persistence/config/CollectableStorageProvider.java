package com.glance.codex.platform.paper.persistence.config;

import com.glance.codex.api.data.storage.CollectableStorage;
import com.glance.codex.platform.paper.persistence.file.FlatFileCollectableStorage;
import com.glance.codex.platform.paper.persistence.file.FlatFileImport;
import com.glance.codex.platform.paper.persistence.sql.JdbiCollectableStorage;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

@Singleton
public class CollectableStorageProvider implements Provider<CollectableStorage> {

    private final Plugin plugin;
    private final CollectableStorageConfig cfg;
    private final Provider<FlatFileCollectableStorage> flat;
    private final Provider<JdbiCollectableStorage> sql;
    private @Nullable CollectableStorage cached;

    @Inject
    public CollectableStorageProvider(
        @NotNull final Plugin plugin,
        @NotNull final CollectableStorageConfig cfg,
        @NotNull final Provider<FlatFileCollectableStorage> flat,
        @NotNull final Provider<JdbiCollectableStorage> sql
    ) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.flat = flat;
        this.sql = sql;
    }

    /**
     * Resolves the configured store, and fails rather than quietly using a different one.
     *
     * Falling back to flat files would show every player an empty journal and then collect new
     * progress in a second place, leaving two stores to reconcile by hand.
     */
    @Override
    public CollectableStorage get() {
        if (this.cached != null) return cached;

        var backend = cfg.backend();
        if (backend == CollectableStorageConfig.Backend.FLATFILE) {
            plugin.getLogger().info("Using FlatFile storage (JSON)");
            return cached = flat.get();
        }

        plugin.getLogger().info("Using SQL storage via JDBI (" + backend + ").");
        cached = sql.get();
        importFlatFiles(cached);
        return cached;
    }

    /** Releases the connection pool, if one was ever opened. Safe to call more than once. */
    public void shutdown() {
        if (!(cached instanceof AutoCloseable closeable)) return;
        try {
            closeable.close();
        } catch (Exception e) {
            plugin.getLogger().warning("Could not close the storage cleanly: " + e.getMessage());
        } finally {
            cached = null;
        }
    }

    /**
     * Carries any player data left by an earlier FLATFILE install into the SQL store.
     *
     * Done here rather than on a command, so a server that has its backend switched under it does
     * not come up with every journal looking empty.
     */
    private void importFlatFiles(@NotNull CollectableStorage target) {
        File directory = new File(plugin.getDataFolder(), cfg.flatFileDir());
        FlatFileImport job = new FlatFileImport(directory, target, plugin.getLogger());
        if (job.pending()) job.run();
    }

}
