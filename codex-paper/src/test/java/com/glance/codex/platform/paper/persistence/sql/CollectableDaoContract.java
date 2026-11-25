package com.glance.codex.platform.paper.persistence.sql;

import com.glance.codex.platform.paper.persistence.config.SqlBootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the storage layer needs of a DAO, run against each dialect in turn.
 *
 * The calls go through {@link CollectableDao} rather than the annotated subinterface, the way the
 * storage layer holds it, so these also prove the extension still resolves the dialect's own
 * statements through the shared supertype.
 */
abstract class CollectableDaoContract {

    protected static final String PLAYER = "8f1d2b7e-0000-4000-8000-000000000001";
    protected static final String NS = "bestiary";

    private SqlBootstrap sql;

    /** Opens a connection, creating the schema, for one test. */
    protected abstract SqlBootstrap connect();

    @BeforeEach
    void open() {
        sql = connect();
        // A shared database keeps its rows between tests, so each starts from an empty table
        sql.jdbi().useHandle(handle -> {
            handle.execute("DELETE FROM collectable_unlocks");
            handle.execute("DELETE FROM collectable_counts");
        });
    }

    @AfterEach
    void close() {
        // Null when the connection was skipped, since an aborted setup still runs the teardown
        if (sql != null) sql.close();
    }

    @Test
    void unlockingTwiceReportsOnlyTheFirstAsNew() {
        assertEquals(1, (int) read(dao -> dao.insertUnlock(PLAYER, NS, "owl", 1000L)));
        assertEquals(0, (int) read(dao -> dao.insertUnlock(PLAYER, NS, "owl", 2000L)));

        assertEquals(Boolean.TRUE, read(dao -> dao.exists(PLAYER, NS, "owl")));
        assertEquals(1000L, (long) read(dao -> dao.loadAll(PLAYER)).getFirst().firstUnlockedAt);
    }

    @Test
    void countsAccumulateAndClampAtZero() {
        write(dao -> dao.addCount(PLAYER, NS, "owl", 3));
        assertEquals(3L, (long) read(dao -> dao.getCount(PLAYER, NS, "owl")));

        write(dao -> dao.addCount(PLAYER, NS, "owl", 4));
        assertEquals(7L, (long) read(dao -> dao.getCount(PLAYER, NS, "owl")));

        write(dao -> dao.addCount(PLAYER, NS, "owl", -100));
        assertEquals(0L, (long) read(dao -> dao.getCount(PLAYER, NS, "owl")));
    }

    @Test
    void anUncountedEntryReadsAsNullRatherThanFailing() {
        assertNull(read(dao -> dao.getCount(PLAYER, NS, "never-seen")));
    }

    @Test
    void countingDoesNotUnlock() {
        write(dao -> dao.addCount(PLAYER, NS, "banshee", 2));

        assertTrue(read(dao -> dao.loadIds(PLAYER, NS)).isEmpty(), "a counted entry was reported as unlocked");
        assertNull(read(dao -> dao.exists(PLAYER, NS, "banshee")));
    }

    @Test
    void idsThatDifferOnlyInCaseStayApart() {
        write(dao -> {
            dao.insertUnlock(PLAYER, NS, "owl", 1000L);
            dao.insertUnlock(PLAYER, NS, "Owl", 2000L);
        });

        assertEquals(2, read(dao -> dao.loadIds(PLAYER, NS)).size(), "the two ids were merged");
    }

    @Test
    void countsLoadPerNamespaceAndAcrossAll() {
        write(dao -> {
            dao.addCount(PLAYER, NS, "owl", 1);
            dao.addCount(PLAYER, NS, "pixie", 5);
            dao.addCount(PLAYER, "spellbinder", "fireball", 9);
        });

        Map<String, Long> inNamespace = read(dao -> dao.loadCounts(PLAYER, NS)).stream()
                .collect(Collectors.toMap(row -> row.id, row -> row.amount));

        assertEquals(Map.of("owl", 1L, "pixie", 5L), inNamespace);
        assertEquals(3, read(dao -> dao.loadAllCounts(PLAYER)).size());
    }

    @Test
    void deletingAnUnlockLeavesNoCountBehind() {
        write(dao -> {
            dao.insertUnlock(PLAYER, NS, "owl", 1000L);
            dao.addCount(PLAYER, NS, "owl", 4);
        });

        assertEquals(1, (int) read(dao -> dao.deleteCount(PLAYER, NS, "owl")));
        assertEquals(1, (int) read(dao -> dao.deleteOne(PLAYER, NS, "owl")));

        assertNull(read(dao -> dao.getCount(PLAYER, NS, "owl")));
        assertTrue(read(dao -> dao.loadIds(PLAYER, NS)).isEmpty());
    }

    @Test
    void clearingANamespaceLeavesTheOthers() {
        write(dao -> {
            dao.insertUnlock(PLAYER, NS, "owl", 1000L);
            dao.insertUnlock(PLAYER, "spellbinder", "fireball", 1000L);
            dao.addCount(PLAYER, NS, "owl", 4);
        });

        assertEquals(1, (int) read(dao -> dao.deleteCountNamespace(PLAYER, NS)));
        assertEquals(1, (int) read(dao -> dao.deleteNamespace(PLAYER, NS)));

        assertTrue(read(dao -> dao.loadIds(PLAYER, NS)).isEmpty());
        assertEquals(1, read(dao -> dao.loadIds(PLAYER, "spellbinder")).size());
    }

    @Test
    void clearingAPlayerTakesBothTables() {
        write(dao -> {
            dao.insertUnlock(PLAYER, NS, "owl", 1000L);
            dao.addCount(PLAYER, NS, "owl", 4);
            dao.deleteAllCountsForPlayer(PLAYER);
            dao.deleteAllForPlayer(PLAYER);
        });

        assertTrue(read(dao -> dao.loadAll(PLAYER)).isEmpty());
        assertTrue(read(dao -> dao.loadAllCounts(PLAYER)).isEmpty());
    }

    @Test
    void replayStampsWithoutDisturbingTheFirstUnlock() {
        write(dao -> {
            dao.insertUnlock(PLAYER, NS, "owl", 1000L);
            dao.updateReplay(PLAYER, NS, "owl", 5000L);
        });

        List<CollectableDao.Row> rows = read(dao -> dao.loadAll(PLAYER));

        assertEquals(1, rows.size());
        assertEquals(1000L, (long) rows.getFirst().firstUnlockedAt);
        assertEquals(5000L, (long) rows.getFirst().lastReplayedAt);
    }

    @Test
    void anEntryNeverReplayedHasNoReplayStamp() {
        write(dao -> dao.insertUnlock(PLAYER, NS, "owl", 1000L));

        assertNull(read(dao -> dao.loadAll(PLAYER)).getFirst().lastReplayedAt);
    }

    private <R> R read(Function<CollectableDao, R> work) {
        return attach(sql.daoType(), work);
    }

    private void write(Consumer<CollectableDao> work) {
        attach(sql.daoType(), dao -> {
            work.accept(dao);
            return null;
        });
    }

    private <E extends CollectableDao, R> R attach(Class<E> type, Function<CollectableDao, R> work) {
        return sql.jdbi().withExtension(type, work::apply);
    }
}
