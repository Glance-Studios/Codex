package com.glance.codex.api.collectable;

import com.glance.codex.api.collectable.config.RepositoryConfig;
import com.glance.codex.utils.lifecycle.Manager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface CollectableManager extends Manager {

    void loadFromConfig(RepositoryConfig config);

    void registerRepository(CollectableRepository repo);

    Collection<CollectableRepository> getRepositories();

    @Nullable CollectableRepository getRepo(@NotNull String namespace);

    @Nullable Collectable get(@NotNull NamespacedKey key);

    CompletableFuture<Boolean> unlock(@NotNull Player player, NamespacedKey key);

    CompletableFuture<Boolean> isUnlocked(@NotNull Player player, NamespacedKey key);

    CompletableFuture<Set<String>> unlockedIds(@NotNull Player player, @NotNull String namespace);

    // TODO: add other registry addition ability either here or only API?

}
