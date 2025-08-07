package com.glance.codex.platform.paper.item;

import com.glance.codex.platform.paper.api.text.PlaceholderService;
import com.glance.codex.platform.paper.config.model.ItemEntry;
import com.glance.codex.platform.paper.config.model.LineWrapOptions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A flexible builder for {@link ItemStack}s that integrates with
 * {@link ItemEntry} model definitions and version-safe text system
 *
 * @author Cammy
 */
public class ItemBuilder {

    private static MiniMessage mm = MiniMessage.miniMessage();

    private final ItemStack item;

    private ItemBuilder(@NotNull ItemStack item) {
        this.item = item;
    }

    /**
     * Creates a new builder with a blank item of the given material
     *
     * @param material the material type
     * @return the builder instance
     */
    public static ItemBuilder of(@NotNull Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    /**
     * Builds an {@link ItemBuilder} from a model entry
     *
     * @param entry  the model defined item entry
     * @param player the player context for formatting (can be null)
     * @return the configured item builder
     */
    public static ItemBuilder fromConfig(
            @NotNull ItemEntry entry,
            @Nullable Player player,
            @Nullable PlaceholderService resolver
    ) {
        LineWrapOptions wrapOptions = (entry.getLineWrap() != null)
                ? entry.getLineWrap()
                : LineWrapOptions.DISABLED;

        ItemBuilder builder = of(entry.getMaterial());

        builder.item.editMeta(meta -> {
            String nameRaw = entry.getDisplayName();
            List<String> loreRaw = entry.getLore();

            if (resolver != null) {
                nameRaw = resolver.apply(nameRaw, player);
                loreRaw = resolver.apply(loreRaw, player);
            }

            Component nameComponent = mm.deserialize(nameRaw);
            List<String> wrappedLoreLines = wrapLoreLines(loreRaw, wrapOptions);
            List<Component> loreComponents = wrappedLoreLines.stream()
                    .map(mm::deserialize)
                    .toList();

            meta.displayName(nameComponent);
            applyLore(meta, loreComponents, entry.mergeMode());

            if (entry.getFlags() != null) entry.getFlags().forEach(meta::addItemFlags);

            // todo check version for this
            if (entry.getCustomModelData() != null && getModelDataMode(meta) == ModelDataMode.INTEGER) {
                meta.setCustomModelData(entry.getCustomModelData());
            }

            if (entry.isGlint()) {
                @NotNull Enchantment fake = (builder.item.getType() == Material.BOW)
                        ? Enchantment.AQUA_AFFINITY
                        : Enchantment.INFINITY;

                meta.addEnchant(fake, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            // todo skull & 1.20.5+ component system (will need updated api version so new util)
        });

        return builder;
    }

    private static void applyLore(
            @NotNull ItemMeta meta,
            @NotNull List<Component> newLore,
            @NotNull ItemEntry.LoreMergeMode mode
    ) {
        List<Component> existing = meta.lore() != null ? meta.lore() : List.of();
        if (existing == null) existing = List.of();

        List<Component> merged = switch (mode) {
            case REPLACE -> newLore;
            case APPEND -> Stream.concat(existing.stream(), newLore.stream()).toList();
            case PREPEND -> Stream.concat(newLore.stream(), existing.stream()).toList();
            case IGNORE_IF_PRESENT -> existing.isEmpty() ? newLore : existing;
        };

        meta.lore(merged);
    }

    private static List<String> wrapLoreLines(@NotNull List<String> rawLore, @NotNull LineWrapOptions opts) {
        List<String> result = new ArrayList<>();

        for (String line : rawLore) {
            if (opts.maxLineLength() == Integer.MAX_VALUE) {
                result.add(line);
                continue;
            }

            String[] words = line.split(" ");
            StringBuilder current = new StringBuilder();

            for (String word : words) {
                if (current.length() + word.length() + 1 > opts.maxLineLength()) {
                    if (!current.isEmpty()) {
                        result.add(current.toString());
                        current.setLength(0);
                    }

                    if (opts.breakWords() && word.length() > opts.maxLineLength()) {
                        for (int i = 0; i < word.length(); i += opts.maxLineLength()) {
                            int end = Math.min(word.length(), i + opts.maxLineLength());
                            result.add(word.substring(i, end));
                        }
                    } else {
                        current.append(word);
                    }
                } else {
                    if (!current.isEmpty()) current.append(" ");
                    current.append(word);
                }
            }

            if (!current.isEmpty()) result.add(current.toString());
        }

        return result;
    }

    /**
     * Determines which model data format is supported on the current server version
     *
     * @param meta the item meta to test
     * @return the supported model data mode
     */
    public static @NotNull ModelDataMode getModelDataMode(@NotNull ItemMeta meta) {
        return ModelDataMode.INTEGER;
    }

    /**
     * Enum indicating which model data format should be used when applying custom visuals
     */
    public enum ModelDataMode {
        INTEGER,
        COMPONENT
    }

    public ItemStack build() {
        return item;
    }

}
