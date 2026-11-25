package com.glance.codex.platform.paper.persistence.sql;

import java.util.List;
import java.util.Set;

/**
 * Every statement the SQL storage needs, without the SQL.
 *
 * Each dialect subinterface carries its own annotated copy of these, so the storage layer can hold
 * one DAO type and stop asking which database it is talking to.
 */
public interface CollectableDao {

    void createTable();

    void createIdxPlayer();

    void createIdxPlayerNs();

    void createCountsTable();

    void createIdxCountsPlayerNs();

    /** @return rows inserted, so zero means the entry was already unlocked */
    int insertUnlock(String player, String namespace, String id, long whenMillis);

    void updateReplay(String player, String namespace, String id, long whenMillis);

    Set<String> loadIds(String player, String namespace);

    Boolean exists(String player, String namespace, String id);

    List<Row> loadAll(String player);

    int deleteOne(String player, String namespace, String id);

    int deleteNamespace(String player, String namespace);

    void deleteAllForPlayer(String player);

    /** Clamped at zero, so a negative [by] cannot take a count below nothing. */
    void addCount(String player, String namespace, String id, long by);

    Long getCount(String player, String namespace, String id);

    List<CountRow> loadCounts(String player, String namespace);

    List<CountRow> loadAllCounts(String player);

    int deleteCount(String player, String namespace, String id);

    int deleteCountNamespace(String player, String namespace);

    void deleteAllCountsForPlayer(String player);

    class CountRow {
        public String namespace;
        public String id;
        public Long amount;
    }

    class Row {
        public String namespace;
        public String id;
        public Long firstUnlockedAt;
        public Long lastReplayedAt;
    }
}
