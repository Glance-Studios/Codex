package com.glance.codex.platform.paper.api.collectable;

import com.glance.codex.platform.paper.api.collectable.config.RepositoryConfig;
import com.glance.codex.utils.lifecycle.Manager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface CollectableManager extends Manager {

    CollectableRepository loadFromConfig(RepositoryConfig<? extends Collectable> config);

    void registerRepository(CollectableRepository repo);

    Collection<CollectableRepository> getRepositories();

    @Nullable Collectable get(@NotNull NamespacedKey key);

    boolean unlock(@NotNull Player player, NamespacedKey key);

    boolean isUnlocked(@NotNull Player player, NamespacedKey key);

}
