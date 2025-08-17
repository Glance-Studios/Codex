package com.glance.codex.platform.paper.collectable.type.note;

import com.glance.codex.api.text.PlaceholderService;
import com.glance.codex.platform.paper.collectable.BaseCollectable;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.google.inject.Inject;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NoteCollectable extends BaseCollectable {

    private final NotebookRegistry notebookRegistry;

    @Inject
    public NoteCollectable(
        @NotNull final NotebookRegistry notebookRegistry,
        @NotNull final PlaceholderService placeholderService
    ) {
        super(placeholderService);
        this.notebookRegistry = notebookRegistry;
    }

    @ConfigField
    private String bookId;

    private void openBook(@NotNull Player player) {
        if (meta == null) throw new IllegalStateException("Collectable Meta is NULL during usage!");
        String foundBookId = bookId == null ? meta.entryId() : bookId;
        NamespacedKey bookKey = new NamespacedKey(meta.namespace(), foundBookId);

        if (notebookRegistry.exists(bookKey)) {
            notebookRegistry.open(bookKey, player);
        }
    }

    @Override
    public void onDiscover(@NotNull Player player) {
        openBook(player);
    }

    @Override
    public void onReplay(@NotNull Player player) {
        openBook(player);
    }

}
