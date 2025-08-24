package com.glance.codex.platform.paper.config.engine.reload;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface ConfigReloader {

    CompletableFuture<ReloadSummary> reloadInstance(@NotNull Config.Handler instance);

    <T extends Config.Handler> CompletableFuture<ReloadSummary> reloadAllOf(Class<T> type);

    CompletableFuture<ReloadSummary> reloadAll();

}
