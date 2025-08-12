package com.glance.codex.platform.paper.api.collectable;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Collectable {

    @NotNull String key();

    @NotNull
    Component displayName();

    @NotNull String rawDisplayName();

    @NotNull
    ItemStack icon(@Nullable OfflinePlayer player);

    boolean showWhenLocked();

    boolean allowReplay();

}
