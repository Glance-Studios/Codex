package com.glance.codex.platform.paper.api.collectable;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface CollectableRepository<T extends Collectable> {

    @NotNull String namespace();

    @NotNull
    Map<String, T> entries();

    @Nullable T get(@NotNull NamespacedKey key);

    default @Nullable T get(@NotNull String id) {
        return get(new NamespacedKey(namespace(), id));
    }

}
