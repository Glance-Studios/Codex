package com.glance.codex.platform.paper.api.collectable;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface Collectable {

    @NotNull
    String namespace();

    @NotNull
    Component displayName();

    @NotNull String rawDisplayName();

    @NotNull
    ItemStack icon();

    boolean showWhenLocked();

    boolean allowReplay();

}
