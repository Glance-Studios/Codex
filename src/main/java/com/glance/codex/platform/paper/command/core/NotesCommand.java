package com.glance.codex.platform.paper.command.core;

import com.glance.codex.platform.paper.command.engine.CommandHandler;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
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

    @Inject
    public NotesCommand(
            @NotNull final NotebookRegistry notes
    ) {
        this.notes = notes;
    }

    @Suggestions("notes-namespaces")
    public List<String> suggestNamespaces(
            final CommandContext<CommandSender> ctx,
            final String input
    ) {
        final String prefix = input == null ? "" : input;
        final String pfx = prefix.toLowerCase(Locale.ROOT);

        return notes.all().keySet().stream()
                .map(NamespacedKey::getNamespace)
                .distinct()
                .filter(ns -> ns.toLowerCase(Locale.ROOT).startsWith(pfx))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Suggestions("notes-ids")
    public List<String> suggestIds(
            final CommandContext<CommandSender> ctx,
            final String input
    ) {
        final String prefix = input == null ? "" : input;
        final String pfx = prefix.toLowerCase(Locale.ROOT);

        final String namespace = ctx.getOrDefault("namespace", "notes");
        if (namespace.isEmpty() || namespace.isBlank()) {
            return List.of();
        }

        return notes.all().keySet().stream()
                .filter(k -> k.getNamespace().equalsIgnoreCase(namespace))
                .map(NamespacedKey::getKey)
                .distinct()
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(pfx))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Command("collectables notes open <player> <namespace> <id>")
    @Permission("collectables.notes.open")
    public void openBook(
            @NotNull CommandSender sender,
            @Argument("player") Player target,
            @Argument(value = "namespace", suggestions = "notes-namespaces") String namespace,
            @Argument(value = "id", suggestions = "notes-ids") String id
    ) {
        notes.open(namespace, id, target);
    }

}
