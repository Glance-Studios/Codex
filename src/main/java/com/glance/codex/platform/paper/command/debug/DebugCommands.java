package com.glance.codex.platform.paper.command.debug;

import com.glance.codex.platform.paper.command.engine.CommandHandler;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.platform.paper.notebooks.book.NoteBookRenderService;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.jetbrains.annotations.NotNull;

@Slf4j
@Singleton
@AutoService(CommandHandler.class)
public class DebugCommands implements CommandHandler {

    private final NotebookRegistry notes;

    @Inject
    public DebugCommands(
            @NotNull final NotebookRegistry notes
    ) {
        this.notes = notes;
    }

    @Command("open-book <namespace> <id>")
    public void openBook(
            @NotNull Player sender,
            @Argument("namespace") String namespace,
            @Argument("id") String id
    ) {
        log.warn("Attempting to present book from {}:{}", namespace, id);
        notes.open(namespace, id, sender);
    }

}
