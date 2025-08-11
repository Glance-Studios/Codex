package com.glance.codex.platform.paper.notebooks;

import com.glance.codex.platform.paper.config.model.BookConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public interface NotebookRegistry {

    /**
     * @return immutable view of all loaded book keyed by id
     */
    Map<NamespacedKey, BookConfig> all();

    /**
     * Register a book of id, under the namespace
     */
    void register(String namespace, String id, BookConfig cfg);

    /**
     * Clear book for the namespace
     */
    void unregisterNamespace(String namespace);

    /**
     * Look up a book by id
     */
    Optional<BookConfig> get(NamespacedKey id);

    boolean exists(NamespacedKey id);

    /**
     * Build a written book and give it to the players inventory
     */
    boolean give(NamespacedKey id, Player player);

    /**
     * Open the book UI for the player
     */
    boolean open(NamespacedKey id, Player player);

    /**
     * Reload from disk
     */
    void reload();

}
