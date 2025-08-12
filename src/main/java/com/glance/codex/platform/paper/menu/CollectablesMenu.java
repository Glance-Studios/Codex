package com.glance.codex.platform.paper.menu;

import com.glance.codex.platform.paper.api.text.PlaceholderService;
import com.glance.codex.platform.paper.config.model.ItemEntry;
import com.glance.codex.platform.paper.item.ItemBuilder;
import com.glance.codex.platform.paper.menu.config.CollectableMenuConfig;
import com.glance.codex.platform.paper.menu.config.codec.SlotSpec;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.triumphteam.gui.container.GuiContainer;
import dev.triumphteam.gui.element.GuiItem;
import dev.triumphteam.gui.paper.Gui;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
@Singleton
public class CollectablesMenu {

    private final CollectableMenuConfig cfg;
    private final PlaceholderService placeholders;
    private final MiniMessage MM = MiniMessage.miniMessage();

    @Inject
    public CollectablesMenu(
            @NotNull final CollectableMenuConfig cfg,
            @NotNull final PlaceholderService placeholders
            ) {
        this.cfg = cfg;
        this.placeholders = placeholders;
    }

    public void open(@NotNull Player player, boolean debug) {
        int rows = Math.max(1, Math.min(6, cfg.rows()));
        int size = rows * 9;

        Gui gui = Gui.of(rows)
                .title(MM.deserialize(cfg.title()))
                .statelessComponent(container -> {
                    applyFiller(container, size, cfg.filler(), player);

                    if (debug) {
                        // Repo area
                        paintSpec(container, cfg.repoSlots(), rows,
                                dev.triumphteam.gui.paper.builder.item.ItemBuilder
                                        .from(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                                        .name(MM.deserialize("<gold>Repo Slots"))
                                        .asGuiItem());

                        // Entry area
                        paintSpec(container, cfg.entrySlots(), rows,
                                dev.triumphteam.gui.paper.builder.item.ItemBuilder
                                        .from(Material.LIME_STAINED_GLASS_PANE)
                                        .name(MM.deserialize("<gold>Entry Slots"))
                                        .asGuiItem());

                        paintSpec(container, cfg.entryNext(), rows,
                                dev.triumphteam.gui.paper.builder.item.ItemBuilder
                                        .from(Material.TRIPWIRE_HOOK)
                                        .name(MM.deserialize("<gold>Entry Next"))
                                        .asGuiItem());
                        paintSpec(container, cfg.entryPrev(), rows,
                                dev.triumphteam.gui.paper.builder.item.ItemBuilder
                                        .from(Material.TRIPWIRE_HOOK)
                                        .name(MM.deserialize("<gold>Entry Prev"))
                                        .asGuiItem());

                        paintSpec(container, cfg.repoPrev(), rows,
                                dev.triumphteam.gui.paper.builder.item.ItemBuilder
                                        .from(Material.ARROW)
                                        .name(MM.deserialize("<gold>Repo Prev"))
                                        .asGuiItem());
                        paintSpec(container, cfg.repoNext(), rows,
                                dev.triumphteam.gui.paper.builder.item.ItemBuilder
                                        .from(Material.ARROW)
                                        .name(MM.deserialize("<gold>Repo Next"))
                                        .asGuiItem());
                    } else {
                        // todo
                    }
                })
                .build();

        gui.open(player);
    }

    private void applyFiller(
            GuiContainer<Player, ItemStack> container,
            int size,
            ItemEntry fillerEntry,
            Player player
    ) {
        ItemStack fillerItem = ItemBuilder.fromConfig(fillerEntry, player, placeholders).build();
        GuiItem<Player, ItemStack> filler = dev.triumphteam.gui.paper.builder.item.ItemBuilder
                .from(fillerItem).asGuiItem((p, ctx) -> {});

        for (int slot = 0; slot < size; slot++) {
            setAtSlot(container, slot, filler);
        }
    }

    private void setAtSlot(
        GuiContainer<Player, ItemStack> container,
        int slot,
        GuiItem<Player, ItemStack> item
    ) {
        final int row = (slot / 9) + 1;
        final int col = (slot % 9) + 1;
        container.setItem(row, col, item);
    }

    private void paintSpec(
        GuiContainer<Player, ItemStack> container,
        SlotSpec spec,
        int rows,
        GuiItem<Player, ItemStack> item
    ) {
        if (spec == null) return;
        List<Integer> slots = spec.resolve(rows);
        for (int slot : slots) setAtSlot(container, slot, item);
    }

}
