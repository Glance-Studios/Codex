package com.glance.codex.platform.paper.command.core;

import com.glance.codex.platform.paper.command.engine.CommandHandler;
import com.glance.codex.platform.paper.command.engine.suggestion.SuggestionHelpers;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.platform.paper.notebooks.edit.BookEditor;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@Slf4j
@Singleton
@AutoService(CommandHandler.class)
public class NotesCommand implements CommandHandler {

    private final NotebookRegistry notes;
    private final BookEditor editor;

    @Inject
    public NotesCommand(
            @NotNull final NotebookRegistry notes,
            @NotNull final BookEditor editor
    ) {
        this.notes = notes;
        this.editor = editor;
    }

    @Suggestions("notes-namespaces")
    public List<String> suggestNamespaces(
            final CommandContext<CommandSender> ctx,
            final String input
    ) {
        return SuggestionHelpers.noteNamespaces(notes, input);
    }

    @Suggestions("notes-ids")
    public List<String> suggestIds(
            final CommandContext<CommandSender> ctx,
            final String input
    ) {
        return SuggestionHelpers.noteIds(notes, ctx.getOrDefault("namespace", "notes"), input);
    }

    @Command("collectables|journal notes open <player> <namespace> <id>")
    @Permission("collectables.notes.open")
    public void openBook(
            @NotNull CommandSender sender,
            @Argument("player") Player target,
            @Argument(value = "namespace", suggestions = "notes-namespaces") String namespace,
            @Argument(value = "id", suggestions = "notes-ids") String id
    ) {
        notes.open(namespace, id, target);
    }

    @Command("collectables|journal notes edit <player> <namespace> <id>")
    @Permission("collectables.admin")
    public void editBook(
            @NotNull CommandSender sender,
            @Argument("player") Player target,
            @Argument(value = "namespace", suggestions = "notes-namespaces") String namespace,
            @Argument(value = "id", suggestions = "notes-ids") String id
    ) {
        NamespacedKey key = new NamespacedKey(namespace, id);

        String failure = editor.give(target, key);
        if (failure != null) {
            sender.sendMessage(failure);
            return;
        }

        target.sendMessage("Editing " + key.asString()
                + ". Hold the book and right click to edit, then sign or close it to save.");

        if (!sender.equals(target)) {
            sender.sendMessage("Gave " + target.getName() + " an editor book for " + key.asString());
        }
    }

}
