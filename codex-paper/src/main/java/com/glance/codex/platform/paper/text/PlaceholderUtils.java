package com.glance.codex.platform.paper.text;

import com.glance.codex.api.collectable.Collectable;
import com.glance.codex.api.collectable.CollectableMeta;
import com.glance.codex.api.collectable.CollectableRepository;
import lombok.experimental.UtilityClass;
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
     *      <li>{@code {namespace}} -> repo namespace (e.g. "notes")</li>
     *      <li>{@code {id}} -> collectable id within the repo (e.g. "mysterious_note")</li>
     *      <li>{@code {key}} -> full key "namespace:id"</li>
     *      <li>{@code {collectable_name_formatted}} -> MiniMessage-ready display name</li>
     *      <li>{@code {collectable_name_plain}} -> Raw display name with no formatting tags</li>
     *      <li>{@code {repo_namespace}} -> same as namespace (alias)</li>
     *      <li>{@code {allow_replay}} -> "true"/"false"</li>
     *      <li>{@code {show_when_locked}} -> "true"/"false"</li>
     *      <li>{@code {repo_name_formatted}} -> MiniMessage-ready display name of the repo</li>
     *      <li>{@code {repo_name_plain}} -> Raw repo display name with no formatting tags</li>
     *  </ul>
     */
    public @NotNull Map<String, String> appendCollectableTags(
        @NotNull NamespacedKey key,
        @NotNull Collectable collectable,
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

        map.put("collectable_name_formatted", collectable.rawDisplayName());
        map.put("collectable_name_plain", collectable.plainDisplayName());
        map.put("allow_replay", String.valueOf(collectable.allowReplay()));
        map.put("show_when_locked", String.valueOf(collectable.showWhenLocked()));

        CollectableMeta meta = collectable.getMeta();
        if (meta != null) {
            CollectableRepository repo = meta.repository();

            map.put("repo_name_formatted", repo.displayNameRaw());
            map.put("repo_name_plain", repo.plainDisplayName());
        }

        return map;
    }

    /**
     * Utility for resolving standard player related placeholders
     * <p>
     * Common tokens:
     * <ul>
     *     <li>{@code {player}} -> player name</li>
     *     <li>{@code {uuid}} -> UUID</li>
     *     <li>{@code {world}} -> world name</li>
     *     <li>{@code {x}}, {@code {y}}, {@code {z}} -> block coordinates</li>
     *     <li>{@code {location}} -> formatted location as {@code (x, y, z)}</li>
     *     <li>{@code {block_location}} -> formatted block location as {@code (blockX, blockY, blockZ)}</li>
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
