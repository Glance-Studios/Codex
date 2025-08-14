package com.glance.codex.platform.paper.api.data.storage;

import com.glance.codex.platform.paper.api.data.PlayerCollectables;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Persistence interface for storing and retrieving player collectable progress
 * <p>
 * Implementations may back this with any storage system (SQL, NoSQL, flat file, etc.)
 * <p>
 * The API is asynchronous, returning {@link CompletableFuture} for all operations
 *
 * @author Cammy
 */
public interface CollectableStorage {

    /**
     * Loads all unlocked IDs for a given player in a namespace
     *
     * @param playerId UUID of the player
     * @param namespace repository namespace
     * @return future with unlocked ID set
     */
    CompletableFuture<Set<String>> loadUnlockedIds(
            @NotNull UUID playerId, @NotNull String namespace);

    /**
     * Persists a newly unlocked collectable for the player
     *
     * @param playerId UUID of the player
     * @param namespace repository namespace
     * @param id entry ID
     * @param whenMillis unlock timestamp (epoch millis)
     * @return future with true if newly unlocked, false if already unlocked
     */
    CompletableFuture<Boolean> putUnlock(
            @NotNull UUID playerId, @NotNull String namespace,
            @NotNull String id, long whenMillis
    );

    /**
     * Records the replay time for a collectable
     *
     * @param playerId UUID of the player
     * @param namespace repository namespace
     * @param id entry ID
     * @param whenMillis replay timestamp (epoch millis)
     * @return future that completes when saved
     */
    CompletableFuture<Void> recordReplay(
            @NotNull UUID playerId, @NotNull String namespace,
            @NotNull String id, long whenMillis
    );

    /**
     * Checks whether a collectable is unlocked for the player
     *
     * @param playerId UUID of the player
     * @param namespace repository namespace
     * @param id entry ID
     * @return future with true if unlocked
     */
    CompletableFuture<Boolean> isUnlocked(
            @NotNull UUID playerId, @NotNull String namespace, @NotNull String id);

    /**
     * Loads a full snapshot of all collectables data for the player
     */
    default CompletableFuture<PlayerCollectables> loadSnapshot(@NotNull UUID playerId) {
        throw new UnsupportedOperationException();
    }

    /**
     * Saves a full snapshot of all collectables data for the player
     */
    default CompletableFuture<Void> saveSnapshot(
            @NotNull UUID playerId, @NotNull PlayerCollectables snapshot
    ) {
        throw new UnsupportedOperationException();
    }

}
