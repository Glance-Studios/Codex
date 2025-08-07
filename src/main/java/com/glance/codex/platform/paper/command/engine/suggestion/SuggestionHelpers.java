package com.glance.codex.platform.paper.command.engine.suggestion;

import lombok.experimental.UtilityClass;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@UtilityClass
public class SuggestionHelpers {

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
