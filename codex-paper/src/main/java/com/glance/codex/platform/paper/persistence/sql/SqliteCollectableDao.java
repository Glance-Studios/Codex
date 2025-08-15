package com.glance.codex.platform.paper.persistence.sql;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Set;

public interface SqliteCollectableDao {

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

    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_collectable_unlocks_player ON collectable_unlocks(player_uuid)")
    void createIdxPlayer();

    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_collectable_unlocks_player_ns ON collectable_unlocks(player_uuid, namespace)")
    void createIdxPlayerNs();

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

    @SqlQuery("""
        SELECT id FROM collectable_unlocks
        WHERE player_uuid=:player AND namespace=:ns
        """)
    Set<String> loadIds(
        @Bind("player") String player,
        @Bind("ns") String namespace);

    @SqlQuery("""
        SELECT 1 FROM collectable_unlocks
        WHERE player_uuid=:player AND namespace=:ns AND id=:id
        LIMIT 1
        """)
    Boolean exists(
        @Bind("player") String player,
        @Bind("ns") String namespace,
        @Bind("id") String id);

    @SqlQuery("""
        SELECT namespace, id, first_unlocked_at AS firstUnlockedAt,
        last_replayed_at AS lastReplayedAt FROM collectable_unlocks WHERE player_uuid=:player
        """)
    List<Row> loadAll(@Bind("player") String player);

    @SqlUpdate("""
        DELETE FROM collectable_unlocks WHERE player_uuid=:player
        """)
    void deleteAllForPlayer(@Bind("player") String player);

    class Row {
        public String namespace;
        public String id;
        public Long firstUnlockedAt;
        public Long lastReplayedAt;
    }
}
