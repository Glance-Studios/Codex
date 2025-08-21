package com.glance.codex.platform.paper.menu.util;

import com.glance.codex.api.collectable.Collectable;
import dev.triumphteam.gui.click.ClickContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public record CollectableClick(
        @NotNull Player viewer,
        @NotNull String entryId,
        @NotNull Collectable collectable,
        int slot,
        boolean unlocked,
        @NotNull ClickContext ctx
) {}
