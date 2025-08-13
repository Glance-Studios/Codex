package com.glance.codex.platform.paper.api.data.storage;

import com.glance.codex.platform.paper.api.data.PlayerCollectables;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CollectableStorage {

    CompletableFuture<Set<String>> loadUnlockedIds(
            @NotNull UUID playerId, @NotNull String namespace);

    CompletableFuture<Boolean> putUnlock(
            @NotNull UUID playerId, @NotNull String namespace,
            @NotNull String id, long whenMillis
    );

    CompletableFuture<Void> recordReplay(
            @NotNull UUID playerId, @NotNull String namespace,
            @NotNull String id, long whenMillis
    );

    CompletableFuture<Boolean> isUnlocked(
            @NotNull UUID playerId, @NotNull String namespace, @NotNull String id);

    default CompletableFuture<PlayerCollectables> loadSnapshot(@NotNull UUID playerId) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<Void> saveSnapshot(
            @NotNull UUID playerId, @NotNull PlayerCollectables snapshot
    ) {
        throw new UnsupportedOperationException();
    }

}
