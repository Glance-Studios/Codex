package com.glance.codex.platform.paper.api.collectable;

import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface CollectableRepository {

    @NotNull String namespace();

    @NotNull
    Map<String, Collectable> entries();

    @NotNull
    ItemStack getRepoIcon(@Nullable OfflinePlayer player);

    @NotNull
    default ItemStack getSelectedIcon(@Nullable OfflinePlayer player) {
        return getRepoIcon(player);
    }

    @Nullable Collectable get(@NotNull NamespacedKey key);

    default @Nullable Collectable get(@NotNull String id) {
        return get(new NamespacedKey(namespace(), id));
    }

}
