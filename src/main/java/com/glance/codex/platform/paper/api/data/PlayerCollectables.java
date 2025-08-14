package com.glance.codex.platform.paper.api.data;

import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Holds all collectable-related unlock and replay data for a single player
 * <p>
 *
 * <h2>Stored Data</h2>
 * <ul>
 *   <li>{@link #unlocks} - Set of unlocked IDs per repository namespace</li>
 *   <li>{@link #firstUnlockedAt} - First-time unlock timestamps per ID</li>
 *   <li>{@link #lastReplayedAt} - Last replay timestamps per ID</li>
 * </ul>
 *
 * <p>All timestamps are stored in epoch milliseconds</p>
 *
 * @author Cammy
 */
@Data
@Accessors(fluent = true)
public class PlayerCollectables {

    /** Map of namespace -> unlocked entry IDs */
    private Map<String, Set<String>> unlocks = new HashMap<>();
    /** Map of namespace -> (ID -> first unlocked time) */
    private Map<String, Map<String, Long>> firstUnlockedAt = new HashMap<>();
    /** Map of namespace -> (ID -> last replayed time) */
    private Map<String, Map<String, Long>> lastReplayedAt = new HashMap<>();

    /**
     * Checks if the given entry ID is unlocked in a namespace
     *
     * @param namespace repository namespace
     * @param id collectable ID
     * @return true if unlocked
     */
    public boolean isUnlocked(@NotNull String namespace, @NotNull String id) {
        return unlocks.getOrDefault(namespace, Set.of()).contains(id);
    }

    /**
     * Checks if the given key is unlocked
     *
     * @param key namespaced key for the collectable
     * @return true if unlocked
     */
    public boolean isUnlocked(@NotNull NamespacedKey key) {
        return isUnlocked(key.getNamespace(), key.getKey());
    }

    /**
     * Marks the given entry as unlocked if not already
     * <p>
     * Records the first-unlocked timestamp if new
     *
     * @param namespace namespace of the entry
     * @param id entry ID
     * @param when unlock timestamp (epoch millis)
     * @return true if newly unlocked, false if already unlocked
     */
    public boolean markUnlock(@NotNull String namespace, @NotNull String id, long when) {
        Set<String> set = unlocks.computeIfAbsent(namespace, k -> new HashSet<>());
        boolean added = set.add(id);
        if (added) {
            firstUnlockedAt.computeIfAbsent(namespace, k -> new HashMap<>()).put(id, when);
        }
        return added;
    }

    /**
     * Marks the given entry as unlocked by namespaced key
     */
    public boolean markUnlock(@NotNull NamespacedKey key, long when) {
        return markUnlock(key.getNamespace(), key.getKey(), when);
    }

    /**
     * Records a replay timestamp for the given entry
     *
     * @param namespace namespace of the entry
     * @param id entry ID
     * @param when replay timestamp (epoch millis)
     */
    public void markReplay(@NotNull String namespace, @NotNull String id, long when) {
        lastReplayedAt.computeIfAbsent(namespace, k -> new HashMap<>()).put(id, when);
    }

    /**
     * Records a replay timestamp for the given namespaced key
     */
    public void markReplay(@NotNull NamespacedKey key, long when) {
        markReplay(key.getNamespace(), key.getKey(), when);
    }

}
