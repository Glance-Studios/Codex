package com.glance.codex.platform.paper.persistence.config;

import com.glance.codex.platform.paper.api.data.storage.CollectableStorage;
import com.glance.codex.platform.paper.persistence.file.FlatFileCollectableStorage;
import com.glance.codex.platform.paper.persistence.sql.JdbiCollectableStorage;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public CollectableStorage get() {
        if (this.cached != null) return cached;

        var backend = cfg.backend();
        if (backend == CollectableStorageConfig.Backend.FLATFILE) {
            plugin.getLogger().info("Using FlatFile storage (JSON)");
            return cached = flat.get();
        }

        if (backend == CollectableStorageConfig.Backend.SQLITE) {
            if (!classPresent("org.sqlite.JDBC")) {
                plugin.getLogger().warning("SQLite driver missing. Falling back to FlatFile.");
                return cached = flat.get();
            }
        } else if (backend == CollectableStorageConfig.Backend.MYSQL) {
            if (!classPresent("com.mysql.cj.jdbc.Driver")) {
                plugin.getLogger().warning("MySQL driver missing. Falling back to FlatFile.");
                return cached = flat.get();
            }
        }

        plugin.getLogger().info("Using SQL storage via JDBI (" + backend + ").");
        return cached = sql.get();
    }

    private boolean classPresent(String name) {
        try { Class.forName(name); return true; } catch (ClassNotFoundException e) { return false; }
    }

}
