package com.glance.codex.platform.paper.notebooks;

import com.glance.codex.platform.paper.config.model.BookConfig;
import com.glance.codex.platform.paper.notebooks.edit.BookSource;
import com.glance.codex.utils.lifecycle.Manager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public interface NotebookRegistry extends Manager {

    /**
     * @return immutable view of all loaded book keyed by id
     */
    Map<NamespacedKey, BookConfig> all();

    /**
     * Register a book of id, under the namespace, recording where it was loaded from
     *
     * @param source the file and section this book came from, or null when it was not
     *               loaded from a file and so cannot be edited in game
     */
    void register(
        @NotNull String namespace,
        @NotNull String id,
        @NotNull BookConfig cfg,
        @Nullable BookSource source
    );

    /**
     * Register a book of id, under the namespace
     */
    default void register(
        @NotNull String namespace,
        @NotNull String id,
        @NotNull BookConfig cfg
    ) {
        register(namespace, id, cfg, null);
    }

    /**
     * Where a registered book was loaded from
     *
     * @return the source, or empty if this book cannot be written back to disk
     */
    Optional<BookSource> sourceOf(@NotNull NamespacedKey id);

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
