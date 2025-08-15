package com.glance.codex.platform.paper.text;

import com.glance.codex.platform.paper.api.collectable.Collectable;
import com.glance.codex.platform.paper.api.collectable.CollectableRepository;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class PlaceholderUtils {

    /**
     * Standard placeholders provided for collectable actions.
     * <p>
     * Includes:
     *
     *  <ul>
     *      <li><code>namespace</code> -> repo namespace (e.g. "notes")</li>
     *      <li><code>id</code> -> collectable id within the repo (e.g. "mysterious_note")</li>
     *      <li><code>key</code> -> full key "namespace:id"</li>
     *      <li><code>collectable_display</code> -> MiniMessage-resolved display name (legacy-friendly plain string)</li>
     *      <li><code>collectable_raw</code> -> raw display name (no MiniMessage formatting)</li>
     *      <li><code>repo_namespace</code> -> same as namespace (alias)</li>
     *      <li><code>allow_replay</code> -> "true"/"false"</li>
     *      <li><code>show_when_locked</code> -> "true"/"false"</li>
     *  </ul>
     */
    public @NotNull Map<String, String> appendCollectableTags(
        @NotNull OfflinePlayer player,
        @NotNull NamespacedKey key,
        @NotNull Collectable collectable,
        @NotNull CollectableRepository repo,
        @Nullable Map<String, String> placeholders
    ) {
        Map<String, String> map = new HashMap<>();
        if (placeholders != null) {
            map.putAll(placeholders);
        }

        map.put("namespace", key.namespace());
        map.put("id", key.getKey());
        map.put("key", key.asString());
        map.put("repo_namespace", key.namespace());

        // Display names as plain strings (safe for commands)
        try {
            map.put("collectable_display", PlainTextComponentSerializer.plainText().serialize(collectable.displayName()));
        } catch (Throwable t) {
            map.put("collectable_display", collectable.rawDisplayName()); // fallback
        }
        map.put("collectable_raw", collectable.rawDisplayName());
        map.put("allow_replay", String.valueOf(collectable.allowReplay()));
        map.put("show_when_locked", String.valueOf(collectable.showWhenLocked()));

        return map;
    }

    /**
     * Utility for resolving standard player related placeholders
     * <p>
     * Common tokens:
     * <ul>
     *     <li>{@code <player>} -> player name</li>
     *     <li>{@code <uuid>} -> UUID</li>
     *     <li>{@code <world>} -> world name</li>
     *     <li>{@code <x>}, {@code <y>}, {@code <z>} -> block coordinates</li>
     *     <li>{@code <location>} -> formatted location as {@code (x, y, z)}</li>
     *     <li>{@code <block_location>} -> formatted block location as {@code (blockX, blockY, blockZ)}</li>
     * </ul>
     */
    public @NotNull Map<String, String> appendPlayerTags(
            @NotNull OfflinePlayer player,
            @Nullable Map<String, String> placeholders
    ) {
        Map<String, String> full = new HashMap<>();
        if (placeholders != null) {
            full.putAll(placeholders);
        }

        full.putIfAbsent("player", player.getName());
        full.putIfAbsent("uuid", player.getUniqueId().toString());
        if (player instanceof Player p) {
            full.putIfAbsent("world", p.getWorld().getName());

            Location loc = p.getLocation();
            full.putIfAbsent("location", String.format("(%f, %f, %f)", loc.getX(), loc.getY(), loc.getZ()));
            full.putIfAbsent("block_location", String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            full.putIfAbsent("x", String.valueOf(loc.getBlockX()));
            full.putIfAbsent("y", String.valueOf(loc.getBlockY()));
            full.putIfAbsent("z", String.valueOf(loc.getBlockZ()));
        }

        return full;
    }

}
