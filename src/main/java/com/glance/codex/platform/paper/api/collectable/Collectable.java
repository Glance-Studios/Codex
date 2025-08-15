package com.glance.codex.platform.paper.api.collectable;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Collectable {

    @NotNull
    Component displayName();

    @NotNull String rawDisplayName();

    @NotNull
    ItemStack iconUnlocked(@Nullable OfflinePlayer player);

    @NotNull
    default ItemStack iconLocked(@Nullable OfflinePlayer player) {
        return iconUnlocked(player);
    }

    boolean showWhenLocked();

    boolean allowReplay();

    void setMeta(@NotNull CollectableMeta meta);

    @Nullable
    CollectableMeta getMeta();

}
