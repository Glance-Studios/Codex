package com.glance.codex.platform.paper.inject;

import com.glance.codex.platform.paper.api.collectable.CollectableManager;
import com.glance.codex.platform.paper.api.collectable.CollectableRepository;
import com.glance.codex.platform.paper.api.collectable.base.BaseCollectableRepository;
import com.glance.codex.platform.paper.api.collectable.base.factory.BaseCollectableRepoFactory;
import com.glance.codex.platform.paper.api.data.storage.CollectableStorage;
import com.glance.codex.platform.paper.api.text.PlaceholderService;
import com.glance.codex.platform.paper.collectable.manager.BaseCollectableManager;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.platform.paper.notebooks.book.BaseNotebookRegistry;
import com.glance.codex.platform.paper.persistence.CollectableStorageConfig;
import com.glance.codex.platform.paper.persistence.file.FlatFileCollectableStorage;
import com.glance.codex.platform.paper.persistence.sql.JdbiCollectableStorage;
import com.glance.codex.platform.paper.text.DefaultPlaceholderService;
import com.glance.codex.platform.paper.text.PapiPlaceholderService;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class CodexModule extends AbstractModule {

    private final Plugin plugin;

    public CodexModule(@NotNull final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // bind plugin instance
        this.bind(Plugin.class).toInstance(plugin);
        this.bind(JavaPlugin.class).toInstance((JavaPlugin) plugin);

        this.bind(CollectableManager.class).to(BaseCollectableManager.class).asEagerSingleton();
        this.bind(NotebookRegistry.class).to(BaseNotebookRegistry.class).asEagerSingleton();

        if (isPapiPresent()) {
            this.bind(PlaceholderService.class).to(PapiPlaceholderService.class).asEagerSingleton();
        } else {
            this.bind(PlaceholderService.class).to(DefaultPlaceholderService.class).asEagerSingleton();
        }

        this.install(new FactoryModuleBuilder()
                .implement(CollectableRepository.class, BaseCollectableRepository.class)
                .build(BaseCollectableRepoFactory.class));
    }

    private boolean isPapiPresent() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    @Provides
    CollectableStorage provideStorage(
        @NotNull CollectableStorageConfig cfg,
        @NotNull Provider<FlatFileCollectableStorage> flat,
        @NotNull Provider<JdbiCollectableStorage> sql,
        @NotNull Plugin plugin
    ) {
        var backend = cfg.backend();

        if (backend == CollectableStorageConfig.Backend.FLATFILE) {
            plugin.getLogger().info("[Collectables] Using FlatFile storage (JSON)");
            return flat.get();
        }

        if (backend == CollectableStorageConfig.Backend.SQLITE) {
            if (!classPresent("org.sqlite.JDBC")) {
                plugin.getLogger().warning("[Collectables] SQLite driver missing. Falling back to FlatFile.");
                return flat.get();
            }
        } else if (backend == CollectableStorageConfig.Backend.MYSQL) {
            if (!classPresent("com.mysql.cj.jdbc.Driver")) {
                plugin.getLogger().warning("[Collectables] MySQL driver missing. Falling back to FlatFile.");
                return flat.get();
            }
        }

        plugin.getLogger().info("[Collectables] Using SQL storage via JDBI (" + backend + ").");
        return sql.get();
    }

    private boolean classPresent(String name) {
        try { Class.forName(name); return true; } catch (ClassNotFoundException e) { return false; }
    }

}
