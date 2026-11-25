package com.glance.codex.platform.paper.persistence.sql;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Set;

public interface SqliteCollectableDao extends CollectableDao {

    @Override
    @SqlUpdate("""
        CREATE TABLE IF NOT EXISTS collectable_unlocks (
          player_uuid TEXT NOT NULL,
          namespace TEXT NOT NULL,
          id TEXT NOT NULL,
          first_unlocked_at INTEGER NOT NULL,
          last_replayed_at INTEGER,
          PRIMARY KEY (player_uuid, namespace, id)
        );
        """)
    void createTable();

    @Override
    @SqlUpdate("""
        CREATE TABLE IF NOT EXISTS collectable_counts (
          player_uuid TEXT NOT NULL,
          namespace TEXT NOT NULL,
          id TEXT NOT NULL,
          amount INTEGER NOT NULL DEFAULT 0,
          PRIMARY KEY (player_uuid, namespace, id)
        );
        """)
    void createCountsTable();

    @Override
    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_collectable_counts_player_ns ON collectable_counts(player_uuid, namespace)")
    void createIdxCountsPlayerNs();

    @Override
    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_collectable_unlocks_player ON collectable_unlocks(player_uuid)")
    void createIdxPlayer();

    @Override
    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_collectable_unlocks_player_ns ON collectable_unlocks(player_uuid, namespace)")
    void createIdxPlayerNs();

    @Override
    @SqlUpdate("""
       INSERT OR IGNORE INTO collectable_unlocks
       (player_uuid, namespace, id, first_unlocked_at)
       VALUES (:player, :ns, :id, :when)
       """)
    int insertUnlock(
        @Bind("player") String player,
        @Bind("ns") String namespace,
        @Bind("id") String id,
        @Bind("when") long whenMillis);

    @Override
    @SqlUpdate("""
        UPDATE collectable_unlocks
        SET last_replayed_at = :when
        WHERE player_uuid=:player AND namespace=:ns AND id=:id
        """)
    void updateReplay(
        @Bind("player") String player,
        @Bind("ns") String namespace,
        @Bind("id") String id,
        @Bind("when") long whenMillis);

    @Override
    @SqlQuery("""
        SELECT id FROM collectable_unlocks
        WHERE player_uuid=:player AND namespace=:ns
        """)
    Set<String> loadIds(
        @Bind("player") String player,
        @Bind("ns") String namespace);

    @Override
    @SqlQuery("""
        SELECT 1 FROM collectable_unlocks
        WHERE player_uuid=:player AND namespace=:ns AND id=:id
        LIMIT 1
        """)
    Boolean exists(
        @Bind("player") String player,
        @Bind("ns") String namespace,
        @Bind("id") String id);

    @Override
    @SqlQuery("""
        SELECT namespace, id, first_unlocked_at AS firstUnlockedAt,
        last_replayed_at AS lastReplayedAt FROM collectable_unlocks WHERE player_uuid=:player
        """)
    List<Row> loadAll(@Bind("player") String player);

    @Override
    @SqlUpdate("""
        DELETE FROM collectable_unlocks
        WHERE player_uuid=:player AND namespace=:ns AND id=:id
        """)
    int deleteOne(
        @Bind("player") String player,
        @Bind("ns") String namespace,
        @Bind("id") String id);

    @Override
    @SqlUpdate("""
        DELETE FROM collectable_unlocks
        WHERE player_uuid=:player AND namespace=:ns
        """)
    int deleteNamespace(
        @Bind("player") String player,
        @Bind("ns") String namespace);

    @Override
    @SqlUpdate("""
        DELETE FROM collectable_unlocks WHERE player_uuid=:player
        """)
    void deleteAllForPlayer(@Bind("player") String player);

    @Override
    @SqlUpdate("""
        INSERT INTO collectable_counts (player_uuid, namespace, id, amount)
        VALUES (:player, :ns, :id, MAX(0, :by))
        ON CONFLICT(player_uuid, namespace, id)
        DO UPDATE SET amount = MAX(0, amount + :by)
        """)
    void addCount(
        @Bind("player") String player,
        @Bind("ns") String namespace,
        @Bind("id") String id,
        @Bind("by") long by);

    @Override
    @SqlQuery("""
        SELECT amount FROM collectable_counts
        WHERE player_uuid=:player AND namespace=:ns AND id=:id
        """)
    Long getCount(
        @Bind("player") String player,
        @Bind("ns") String namespace,
        @Bind("id") String id);

    @Override
    @SqlQuery("""
        SELECT id, amount FROM collectable_counts
        WHERE player_uuid=:player AND namespace=:ns
        """)
    List<CountRow> loadCounts(
        @Bind("player") String player,
        @Bind("ns") String namespace);

    @Override
    @SqlQuery("""
        SELECT namespace, id, amount FROM collectable_counts
        WHERE player_uuid=:player
        """)
    List<CountRow> loadAllCounts(@Bind("player") String player);

    @Override
    @SqlUpdate("""
        DELETE FROM collectable_counts
        WHERE player_uuid=:player AND namespace=:ns AND id=:id
        """)
    int deleteCount(
        @Bind("player") String player,
        @Bind("ns") String namespace,
        @Bind("id") String id);

    @Override
    @SqlUpdate("""
        DELETE FROM collectable_counts
        WHERE player_uuid=:player AND namespace=:ns
        """)
    int deleteCountNamespace(
        @Bind("player") String player,
        @Bind("ns") String namespace);

    @Override
    @SqlUpdate("""
        DELETE FROM collectable_counts WHERE player_uuid=:player
        """)
    void deleteAllCountsForPlayer(@Bind("player") String player);
}
