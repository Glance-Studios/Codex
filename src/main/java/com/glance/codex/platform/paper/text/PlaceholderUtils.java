package com.glance.codex.platform.paper.text;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class PlaceholderUtils {

    /**
     * Utility for resolving standard player related placeholders
     * <p>
     * Common tokens:
     * <ul>
     *     <li>{@code <player>} -> player name</li>
     *     <li>{@code <uuid>} -> UUID</li>
     *     <li>{@code <world>} -> world name</li>
     *     <li>{@code <x>}, {@code <y>}, {@code <z>} → block coordinates</li>
     *     <li>{@code <location>} -> formatted location as {@code (x, y, z)}</li>
     *     <li>{@code <block_location>} -> formatted block location as {@code (blockX, blockY, blockZ)}</li>
     * </ul>
     */
    public @NotNull Map<String, String> appendPlayerResolver(@NotNull Player player, @Nullable Map<String, String> placeholders) {
        Map<String, String> full = new HashMap<>();
        if (placeholders != null) {
            full.putAll(placeholders);
        }

        full.putIfAbsent("player", player.getName());
        full.putIfAbsent("uuid", player.getUniqueId().toString());
        full.putIfAbsent("world", player.getWorld().getName());

        Location loc = player.getLocation();
        full.putIfAbsent("location", String.format("(%f, %f, %f)", loc.getX(), loc.getY(), loc.getZ()));
        full.putIfAbsent("block_location", String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        full.putIfAbsent("x", String.valueOf(loc.getBlockX()));
        full.putIfAbsent("y", String.valueOf(loc.getBlockY()));
        full.putIfAbsent("z", String.valueOf(loc.getBlockZ()));

        return full;
    }

}
