package com.glance.codex.platform.paper.menu;

import com.glance.codex.api.collectable.CollectableManager;
import dev.triumphteam.nova.MutableState;
import dev.triumphteam.nova.holder.StateHolder;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ViewModel for the {@code /collectables} menu
 * <p>
 * Holds all UI-related state for the currently open collectables GUI,
 * such as the selected repository index, current page indices,
 * cached unlocked entry IDs, and any namespaces currently being loaded
 *
 * @author Cammy
 */
@AllArgsConstructor
final class CollectablesVM {

    final Plugin plugin;
    final CollectableManager manager;
    final Player player;

    /** Currently selected repository index */
    final MutableState<Integer> repoIndex;
    /** Current page index for repository list */
    final MutableState<Integer> repoPage;
    final MutableState<Integer> entryPage;

    /** Map of namespace -> unlocked entry IDs for the player */
    final MutableState<ConcurrentHashMap<String, Set<String>>> unlockedMap;
    /** Namespaces currently being loaded from storage */
    final MutableState<Set<String>> loading;
    /** Incremented to force a GUI re-render when state changes */
    final MutableState<Integer> tick;

    /**
     * Creates and stores a {@link CollectablesVM} in the provided state holder
     *
     * @param holder state holder from Triumph GUI component
     * @param plugin owning plugin instance
     * @param manager collectable manager
     * @param player player viewing the menu
     * @param initialRepoIdx initial selected repo index
     * @param initialNs initial namespace
     * @param initialUnlocked unlocked IDs for the initial namespace
     * @return new VM instance bound to the state holder
     */
    static CollectablesVM remember(
        @NotNull StateHolder holder,
        @NotNull Plugin plugin,
        @NotNull CollectableManager manager,
        @NotNull Player player,
        int initialRepoIdx,
        String initialNs,
        Set<String> initialUnlocked
    ) {
        var vm = new CollectablesVM(
            plugin, manager, player,
            holder.remember(initialRepoIdx),
            holder.remember(0),
            holder.remember(0),
            holder.remember(new ConcurrentHashMap<>()),
            holder.remember(Collections.newSetFromMap(new ConcurrentHashMap<>())),
            holder.remember(0)
        );
        vm.unlockedMap.get().put(initialNs, new HashSet<>(initialUnlocked));
        return vm;
    }

    /**
     * Forces the GUI to repaint by incrementing the tick counter
     */
    void forceRepaint() { tick.set(tick.get() + 1); }

    /**
     * @return true if the unlocked set for the given namespace is cached
     */
    boolean hasUnlocked(String ns) { return unlockedMap.get().containsKey(ns); }

    /**
     * @return true if the given namespace is currently loading from storage
     */
    boolean isLoading(String ns) { return loading.get().contains(ns); }

    /**
     * Gets the unlocked IDs for the given namespace, or an empty set if none cached
     */
    Set<String> unlocked(String ns) { return unlockedMap.get().getOrDefault(ns, Collections.emptySet()); }

    /**
     * Marks a single collectable as unlocked for the given namespace and triggers a repaint
     */
    void markUnlocked(String ns, String id) {
        unlockedMap.get().computeIfAbsent(ns, k -> new HashSet<>()).add(id);
        forceRepaint();
    }

    /**
     * Ensures unlocked IDs for a namespace are loaded
     * <p>
     * If not cached and not already loading, starts an async fetch via the manager,
     * caches the results on the main thread, and triggers a repaint when done
     */
    void ensureLoaded(String ns) {
        if (hasUnlocked(ns) || isLoading(ns)) return;
        loading.get().add(ns);
        manager.unlockedIds(player, ns).whenComplete((ids, err) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        if (err == null) {
                            unlockedMap.get().put(ns, new HashSet<>(ids));
                            forceRepaint();
                        } else {
                            plugin.getLogger().warning("[Collectables] Fetch failed ns=" + ns + ": " + err.getMessage());
                        }
                    } finally {
                        loading.get().remove(ns);
                    }
                })
        );
    }

}
