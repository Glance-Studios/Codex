package com.glance.codex.platform.paper.command.engine.suggestion;

import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import lombok.experimental.UtilityClass;
import org.bukkit.NamespacedKey;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Shared suggestion lookups
 * <p>
 * Cloud registers {@code @Suggestions} providers as it parses each handler class, so a name
 * declared in one class is not reliably visible to another. Handlers therefore declare their
 * own uniquely named provider and delegate the actual lookup here
 */
@UtilityClass
public class SuggestionHelpers {

    /** Namespaces of every registered note book, filtered by prefix. */
    public List<String> noteNamespaces(
            @NotNull NotebookRegistry notes,
            @Nullable String input
    ) {
        final String pfx = (input == null ? "" : input).toLowerCase(Locale.ROOT);

        return notes.all().keySet().stream()
                .map(NamespacedKey::getNamespace)
                .distinct()
                .filter(ns -> ns.toLowerCase(Locale.ROOT).startsWith(pfx))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /** Book ids within a namespace, filtered by prefix. */
    public List<String> noteIds(
            @NotNull NotebookRegistry notes,
            @Nullable String namespace,
            @Nullable String input
    ) {
        if (namespace == null || namespace.isBlank()) return List.of();
        final String pfx = (input == null ? "" : input).toLowerCase(Locale.ROOT);

        return notes.all().keySet().stream()
                .filter(k -> k.getNamespace().equalsIgnoreCase(namespace))
                .map(NamespacedKey::getKey)
                .distinct()
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(pfx))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public <C> SuggestionProvider<C> ofStrings(List<String> values) {
        return (ctx, input) -> CompletableFuture.completedFuture(
                values.stream().map(Suggestion::suggestion).toList()
        );
    }

    public <C> SuggestionProvider<C> ofStrings(String... values) {
        return (ctx, input) -> CompletableFuture.completedFuture(
                Stream.of(values).map(Suggestion::suggestion).toList()
        );
    }

}
