package com.glance.codex.platform.paper.api.collectable;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface CollectableRepository {

    @NotNull String namespace();

    @NotNull
    Map<String, Collectable> entries();

    @Nullable Collectable get(@NotNull NamespacedKey key);

    default @Nullable Collectable get(@NotNull String id) {
        return get(new NamespacedKey(namespace(), id));
    }

}
