package com.glance.codex.api.collectable;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface CollectableRepository {

    @NotNull String namespace();

    @NotNull
    Component displayName();

    @NotNull String displayNameRaw();

    @NotNull String plainDisplayName();

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
