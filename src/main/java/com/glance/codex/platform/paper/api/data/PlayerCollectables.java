package com.glance.codex.platform.paper.api.data;

import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@Accessors(fluent = true)
public class PlayerCollectables {

    private Map<String, Set<String>> unlocks = new HashMap<>();
    private Map<String, Map<String, Long>> firstUnlockedAt = new HashMap<>();
    private Map<String, Map<String, Long>> lastReplayedAt = new HashMap<>();

    public boolean isUnlocked(@NotNull String namespace, @NotNull String id) {
        return unlocks.getOrDefault(namespace, Set.of()).contains(id);
    }

    public boolean isUnlocked(@NotNull NamespacedKey key) {
        return isUnlocked(key.getNamespace(), key.getKey());
    }

    public boolean markUnlock(@NotNull String namespace, @NotNull String id, long when) {
        Set<String> set = unlocks.computeIfAbsent(namespace, k -> new HashSet<>());
        boolean added = set.add(id);
        if (added) {
            firstUnlockedAt.computeIfAbsent(namespace, k -> new HashMap<>()).put(id, when);
        }
        return added;
    }

    public boolean markUnlock(@NotNull NamespacedKey key, long when) {
        return markUnlock(key.getNamespace(), key.getKey(), when);
    }

    public void markReplay(@NotNull String namespace, @NotNull String id, long when) {
        lastReplayedAt.computeIfAbsent(namespace, k -> new HashMap<>()).put(id, when);
    }

    public void markReplay(@NotNull NamespacedKey key, long when) {
        markReplay(key.getNamespace(), key.getKey(), when);
    }

}
