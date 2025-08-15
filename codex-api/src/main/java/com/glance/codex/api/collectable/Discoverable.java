package com.glance.codex.api.collectable;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface Discoverable {
    void onDiscover(@NotNull Player player);
    void onReplay(@NotNull Player player);
}
