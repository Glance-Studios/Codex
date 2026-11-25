package com.glance.codex.platform.paper.persistence.sql;

import com.glance.codex.api.data.PlayerCollectables;
import com.glance.codex.api.data.storage.CollectableStorage;
import com.glance.codex.platform.paper.persistence.config.SqlBootstrap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jdbi.v3.core.Handle;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

@Singleton
public class JdbiCollectableStorage implements CollectableStorage, AutoCloseable {

    private final SqlBootstrap sql;

    @Inject
    public JdbiCollectableStorage(@NotNull final SqlBootstrap bootstrap) {
        this.sql = bootstrap;
    }

    @Override
    public void close() {
        sql.close();
    }

    @Override
    public CompletableFuture<Set<String>> loadUnlockedIds(@NotNull UUID playerId, @NotNull String namespace) {
        return CompletableFuture.supplyAsync(() ->
                read(dao -> dao.loadIds(playerId.toString(), namespace)));
    }

    @Override
    public CompletableFuture<Boolean> putUnlock(@NotNull UUID playerId, @NotNull String namespace, @NotNull String id, long whenMillis) {
        return CompletableFuture.supplyAsync(() ->
                read(dao -> dao.insertUnlock(playerId.toString(), namespace, id, whenMillis)) > 0);
    }

    @Override
    public CompletableFuture<Void> recordReplay(@NotNull UUID playerId, @NotNull String namespace, @NotNull String id, long whenMillis) {
        return CompletableFuture.runAsync(() ->
                write(dao -> dao.updateReplay(playerId.toString(), namespace, id, whenMillis)));
    }

    @Override
    public CompletableFuture<Boolean> deleteUnlock(@NotNull UUID playerId, @NotNull String namespace, @NotNull String id) {
        return CompletableFuture.supplyAsync(() -> read(dao -> {
            dao.deleteCount(playerId.toString(), namespace, id);
            return dao.deleteOne(playerId.toString(), namespace, id);
        }) > 0);
    }

    @Override
    public CompletableFuture<Integer> clearNamespace(@NotNull UUID playerId, @NotNull String namespace) {
        return CompletableFuture.supplyAsync(() -> read(dao -> {
            dao.deleteCountNamespace(playerId.toString(), namespace);
            return dao.deleteNamespace(playerId.toString(), namespace);
        }));
    }

    @Override
    public CompletableFuture<Void> clearAll(@NotNull UUID playerId) {
        return CompletableFuture.runAsync(() -> write(dao -> {
            dao.deleteAllCountsForPlayer(playerId.toString());
            dao.deleteAllForPlayer(playerId.toString());
        }));
    }

    @Override
    public CompletableFuture<Boolean> isUnlocked(@NotNull UUID playerId, @NotNull String namespace, @NotNull String id) {
        return CompletableFuture.supplyAsync(() -> Boolean.TRUE.equals(
                read(dao -> dao.exists(playerId.toString(), namespace, id))));
    }

    @Override
    public CompletableFuture<Long> addCount(
        @NotNull UUID playerId, @NotNull String namespace, @NotNull String id, long by
    ) {
        // One transaction, so the amount read back is the one this change produced
        return CompletableFuture.supplyAsync(() -> inTransaction(dao -> {
            dao.addCount(playerId.toString(), namespace, id, by);
            Long now = dao.getCount(playerId.toString(), namespace, id);
            return now == null ? 0L : now;
        }));
    }

    @Override
    public CompletableFuture<Long> getCount(
        @NotNull UUID playerId, @NotNull String namespace, @NotNull String id
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Long amount = read(dao -> dao.getCount(playerId.toString(), namespace, id));
            return amount == null ? 0L : amount;
        });
    }

    @Override
    public CompletableFuture<Map<String, Long>> loadCounts(
        @NotNull UUID playerId, @NotNull String namespace
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<CollectableDao.CountRow> rows =
                    read(dao -> dao.loadCounts(playerId.toString(), namespace));

            Map<String, Long> counts = new HashMap<>();
            for (CollectableDao.CountRow row : rows) {
                counts.put(row.id, row.amount == null ? 0L : row.amount);
            }
            return counts;
        });
    }

    @Override
    public CompletableFuture<PlayerCollectables> loadSnapshot(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerCollectables snapshot = new PlayerCollectables();

            for (CollectableDao.Row r : read(dao -> dao.loadAll(playerId.toString()))) {
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

            for (CollectableDao.CountRow r : read(dao -> dao.loadAllCounts(playerId.toString()))) {
                snapshot.counts()
                        .computeIfAbsent(r.namespace, k -> new HashMap<>())
                        .put(r.id, r.amount == null ? 0L : r.amount);
            }

            return snapshot;
        });
    }

    @Override
    public CompletableFuture<Void> saveSnapshot(@NotNull UUID playerId, @NotNull PlayerCollectables snapshot) {
        return CompletableFuture.runAsync(() -> inTransaction(dao -> {
            String uuid = playerId.toString();
            dao.deleteAllForPlayer(uuid);
            dao.deleteAllCountsForPlayer(uuid);
            snapshot.counts().forEach((ns, byId) ->
                    byId.forEach((id, amount) -> dao.addCount(uuid, ns, id, amount)));
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
            return null;
        }));
    }

    private <R> R read(@NotNull Function<CollectableDao, R> work) {
        return attach(sql.daoType(), work);
    }

    private void write(@NotNull Consumer<CollectableDao> work) {
        attach(sql.daoType(), dao -> {
            work.accept(dao);
            return null;
        });
    }

    private <R> R inTransaction(@NotNull Function<CollectableDao, R> work) {
        return sql.jdbi().inTransaction(handle -> work.apply(attached(handle, sql.daoType())));
    }

    // Generic, so the configured DAO type is captured and every call resolves against one interface
    private <E extends CollectableDao, R> R attach(
        @NotNull Class<E> type, @NotNull Function<CollectableDao, R> work
    ) {
        return sql.jdbi().withExtension(type, work::apply);
    }

    private <E extends CollectableDao> CollectableDao attached(
        @NotNull Handle handle, @NotNull Class<E> type
    ) {
        return handle.attach(type);
    }
}
