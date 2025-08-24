package com.glance.codex.platform.paper.notebooks;

import com.glance.codex.platform.paper.config.model.BookConfig;
import com.glance.codex.utils.lifecycle.Manager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public interface NotebookRegistry extends Manager {

    /**
     * @return immutable view of all loaded book keyed by id
     */
    Map<NamespacedKey, BookConfig> all();

    /**
     * Register a book of id, under the namespace
     */
    void register(
        @NotNull String namespace,
        @NotNull String id,
        @NotNull BookConfig cfg
    );

    /**
     * Clear book for the namespace
     */
    void unregisterNamespace(@NotNull String namespace);

    /**
     * Look up a book by id
     */
    Optional<BookConfig> get(@NotNull NamespacedKey id);

    boolean exists(@NotNull NamespacedKey id);

    /**
     * Build a written book and give it to the players inventory
     */
    boolean give(
        @NotNull NamespacedKey id,
        @NotNull Player player
    );

    /**
     * Open the book UI for the player
     */
    boolean open(
        @NotNull NamespacedKey id,
        @NotNull Player player
    );

    /**
     * Open the book UI for the player
     */
    default boolean open(
        @NotNull String namespace,
        @NotNull String id,
        @NotNull Player player
    ) {
        return open(new NamespacedKey(namespace, id), player);
    }

}
