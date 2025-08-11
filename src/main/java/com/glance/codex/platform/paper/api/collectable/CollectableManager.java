package com.glance.codex.platform.paper.api.collectable;

import com.glance.codex.platform.paper.api.collectable.config.RepositoryConfig;
import com.glance.codex.utils.lifecycle.Manager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CollectableManager extends Manager {

    CollectableRepository loadFromConfig(RepositoryConfig config);

    void registerRepository(CollectableRepository repo);

    @Nullable Collectable get(@NotNull NamespacedKey key);

    boolean unlock(@NotNull Player player, NamespacedKey key);

    boolean isUnlocked(@NotNull Player player, NamespacedKey key);

}
