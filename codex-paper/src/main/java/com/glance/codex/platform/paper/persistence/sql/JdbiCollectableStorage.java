package com.glance.codex.platform.paper.persistence.sql;

import com.glance.codex.platform.paper.api.data.PlayerCollectables;
import com.glance.codex.api.data.storage.CollectableStorage;
import com.glance.codex.platform.paper.persistence.config.SqlBootstrap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Singleton
public class JdbiCollectableStorage implements CollectableStorage {

    private final SqlBootstrap sql;

    @Inject
    public JdbiCollectableStorage(
        @NotNull final Plugin plugin,
        @NotNull final SqlBootstrap bootstrap
    ) {
        this.sql = bootstrap;

        if (isSqlite()) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("""
                Selected SQLite but No SQLite Driver found.
                Options:
                  - Use the '-with-sqlite' jar of CollectablesCodex, or
                  - Install a dedicated SQLite driver plugin, or
                  - Switch backend to FLATFILE in config.
                """);
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isSqlite() {
        return this.sql.dialect() == SqlBootstrap.Dialect.SQLITE;
    }

    @Override
    public CompletableFuture<Set<String>> loadUnlockedIds(@NotNull UUID playerId, @NotNull String namespace) {
        return CompletableFuture.supplyAsync(() -> {
            if (isSqlite()) {
                return sql.jdbi().withExtension(SqliteCollectableDao.class,
                        dao -> dao.loadIds(playerId.toString(), namespace));
            } else {
                throw new UnsupportedOperationException();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> putUnlock(@NotNull UUID playerId, @NotNull String namespace, @NotNull String id, long whenMillis) {
        return CompletableFuture.supplyAsync(() -> {
            int changed = isSqlite()
                    ? sql.jdbi().withExtension(SqliteCollectableDao.class,
                        dao -> dao.insertUnlock(playerId.toString(), namespace, id, whenMillis))
                    : 0;
            return changed > 0;
        });
    }

    @Override
    public CompletableFuture<Void> recordReplay(@NotNull UUID playerId, @NotNull String namespace, @NotNull String id, long whenMillis) {
        return CompletableFuture.runAsync(() -> {
            if (isSqlite()) {
                sql.jdbi().useExtension(SqliteCollectableDao.class,
                        dao -> dao.updateReplay(playerId.toString(), namespace, id, whenMillis));
            } else {
                throw new UnsupportedOperationException();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isUnlocked(@NotNull UUID playerId, @NotNull String namespace, @NotNull String id) {
        return CompletableFuture.supplyAsync(() -> {
            Boolean result = isSqlite()
                    ? sql.jdbi().withExtension(SqliteCollectableDao.class,
                        dao -> dao.exists(playerId.toString(), namespace, id))
                    : false;
            return Boolean.TRUE.equals(result);
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<PlayerCollectables> loadSnapshot(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            List<?> rows =
                isSqlite()
                ? sql.jdbi().withExtension(SqliteCollectableDao.class, dao -> dao.loadAll(playerId.toString()))
                : List.of();

            PlayerCollectables snapshot = new PlayerCollectables();
            if (rows.isEmpty()) return snapshot;

            if (rows.getFirst() instanceof SqliteCollectableDao.Row) {
                for (SqliteCollectableDao.Row r : (List<SqliteCollectableDao.Row>) rows) {
                    snapshot.unlocks().computeIfAbsent(r.namespace, k -> new HashSet<>()).add(r.id);
                    if (r.firstUnlockedAt != null) {
                        snapshot.firstUnlockedAt()
                                .computeIfAbsent(r.namespace, k -> new HashMap<>()).put(r.id, r.firstUnlockedAt);
                    }
                    if (r.lastReplayedAt != null) {
                        snapshot.lastReplayedAt()
                                .computeIfAbsent(r.namespace, k -> new HashMap<>()).put(r.id, r.lastReplayedAt);
                    }
                }
            } else {
                throw new UnsupportedOperationException();
            }

            return snapshot;
        });
    }

    @Override
    public CompletableFuture<Void> saveSnapshot(@NotNull UUID playerId, @NotNull PlayerCollectables snapshot) {
        return CompletableFuture.runAsync(() -> {
           sql.jdbi().useTransaction(handle -> {
               String uuid = playerId.toString();
               if (isSqlite()) {
                   var dao = handle.attach(SqliteCollectableDao.class);
                   dao.deleteAllForPlayer(uuid);
                   long now = System.currentTimeMillis();
                   snapshot.unlocks().forEach((ns, ids) -> {
                       for (String id : ids) {
                           long first = snapshot.firstUnlockedAt()
                                   .getOrDefault(ns, Map.of())
                                   .getOrDefault(id, now);
                           dao.insertUnlock(uuid, ns, id, first);
                           Long replay = snapshot.lastReplayedAt()
                                   .getOrDefault(ns, Map.of())
                                   .get(id);
                           if (replay != null) dao.updateReplay(uuid, ns, id, replay);
                       }
                   });
               } else {
                   throw new UnsupportedOperationException();
               }
           });
        });
    }
}
