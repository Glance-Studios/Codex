package com.glance.codex.platform.paper.menu;

import com.glance.codex.platform.paper.api.collectable.Collectable;
import com.glance.codex.platform.paper.api.collectable.CollectableManager;
import com.glance.codex.platform.paper.api.collectable.CollectableRepository;
import com.glance.codex.platform.paper.api.collectable.base.BaseCollectable;
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
import dev.triumphteam.nova.MutableState;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Singleton
public class CollectablesMenu {

    private final CollectableMenuConfig cfg;
    private final PlaceholderService placeholders;
    private final CollectableManager collectableManager;

    private final MiniMessage MM = MiniMessage.miniMessage();

    @Inject
    public CollectablesMenu(
            @NotNull final CollectableMenuConfig cfg,
            @NotNull final PlaceholderService placeholders,
            @NotNull final CollectableManager collectableManager
    ) {
        this.cfg = cfg;
        this.placeholders = placeholders;
        this.collectableManager = collectableManager;
    }

    public void open(@NotNull Player player, boolean debug) {
        int rows = Math.max(1, Math.min(6, cfg.rows()));
        int size = rows * 9;

        Gui gui = Gui.of(rows)
                .title(MM.deserialize(cfg.title()))
                .component(component -> {
                    final var repoIndex = component.remember(0);
                    final var repoPage = component.remember(0);
                    final var entryPage = component.remember(0);

                    component.render(container -> {
                        applyFiller(container, size, cfg.filler(), player);

                        if (debug) {
                            paintDebug(container, rows);
                        } else {
                            List<CollectableRepository> repos = new ArrayList<>(collectableManager.getRepositories());
                            if (repos.isEmpty()) return;

                            int selectedRepoIdx = Math.max(0, Math.min(repoIndex.get(), repos.size() - 1));
                            CollectableRepository repo = repos.get(selectedRepoIdx);

                            paintRepos(container, player, repos, repo, repoIndex, repoPage);

                            paintEntries(container, player, repo, entryPage);
                        }
                    });
                })
                .build();

        gui.open(player);
    }

    private void paintEntries(
        GuiContainer<Player, ItemStack> container,
        Player player,
        CollectableRepository repo,
        MutableState<Integer> entryPage
    ) {
        final List<Integer> slots = cfg.entrySlots().resolve(cfg.rows());
        if (slots.isEmpty()) return;

        final List<Collectable> all = new ArrayList<>(repo.entries().values());
        // todo any kind of sort available?

        final int perPage = slots.size();
        final int maxPage = computeMaxPage(all.size(), perPage);
        final int page = Math.max(0, Math.min(entryPage.get(), maxPage));
        final int start = page * perPage;
        final int end = Math.min(start + perPage, all.size());

        for (int i = start; i < end; i++) {
            final int slot = slots.get(i - start);
            final Collectable c = all.get(i);
            final NamespacedKey key = new NamespacedKey(repo.namespace(), c.key());
            final boolean unlocked = collectableManager.isUnlocked(player, key);

            // todo things based on unlocked or not

            GuiItem<Player, ItemStack> icon = dev.triumphteam.gui.paper.builder.item.ItemBuilder.from(c.icon(player))
                    .asGuiItem((v, ctx) -> {
                        // TODO
                    });

            setAtSlot(container, slot, icon);
        }

        final boolean paging = needsPaging(all.size(), perPage);
        final boolean showPrev = paging && page > 0;
        final boolean showNext = paging && page < maxPage;

        placeIf(showPrev, container, cfg.entryPrev(), () -> {
            final ItemStack prev = ItemBuilder.fromConfig(cfg.entryPrevItem(), player, placeholders).build();
            return dev.triumphteam.gui.paper.builder.item.ItemBuilder.from(prev).asGuiItem((v, ctx) ->
                    entryPage.update(p -> Math.max(0, p - 1)));
        });

        placeIf(showNext, container, cfg.entryNext(), () -> {
            final ItemStack next = ItemBuilder.fromConfig(cfg.entryNextItem(), player, placeholders).build();
            return dev.triumphteam.gui.paper.builder.item.ItemBuilder.from(next).asGuiItem((v, ctx) ->
                    entryPage.update(p -> Math.min(maxPage, p + 1)));
        });
    }

    private void paintRepos(
        GuiContainer<Player, ItemStack> container,
        Player player,
        List<CollectableRepository> repos,
        CollectableRepository selectedRepo,
        MutableState<Integer> repoIndex,
        MutableState<Integer> repoPage
    ) {
        final List<Integer> slots = cfg.repoSlots().resolve(cfg.rows());
        if (slots.isEmpty() || repos.isEmpty()) return;

        final int perPage = slots.size();
        final int maxPage = Math.max(0, (repos.size() - 1) / perPage);
        final int page = Math.max(0, Math.min(repoPage.get(), maxPage));
        final int start = page * perPage;
        final int end = Math.min(start + perPage, repos.size());

        // render page
        for (int i = start; i < end; i++) {
            final int slot = slots.get(i - start);
            final CollectableRepository repo = repos.get(i);

            final ItemStack icon = (repo == selectedRepo)
                    ? repo.getSelectedIcon(player)
                    : repo.getRepoIcon(player);

            // todo things when repo not unlocked for player

            int finalI = i;
            final GuiItem<Player, ItemStack> btn = dev.triumphteam.gui.paper.builder.item.ItemBuilder
                    .from(icon).asGuiItem((v, ctx) -> repoIndex.update(old -> finalI));

            setAtSlot(container, slot, btn);
        }

        final boolean paging = needsPaging(repos.size(), perPage);
        final boolean showPrev = paging && page > 0;
        final boolean showNext = paging && page < maxPage;

        placeIf(showPrev, container, cfg.repoPrev(), () -> {
            final ItemStack prev = ItemBuilder.fromConfig(cfg.repoPrevItem(), player, placeholders).build();
            return dev.triumphteam.gui.paper.builder.item.ItemBuilder.from(prev).asGuiItem((v, ctx) ->
                    repoPage.update(p -> Math.max(0, p - 1)));
        });

        placeIf(showNext, container, cfg.repoNext(), () -> {
            final ItemStack next = ItemBuilder.fromConfig(cfg.repoNextItem(), player, placeholders).build();
            return dev.triumphteam.gui.paper.builder.item.ItemBuilder.from(next).asGuiItem((v, ctx) ->
                    repoPage.update(p -> Math.min(maxPage, p + 1)));
        });
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

    private static int computeMaxPage(int total, int perPage) {
        if (perPage <= 0 || total <= 0) return 0;
        return Math.max(0, (total - 1) / perPage);
    }

    private static boolean needsPaging(int total, int perPage) {
        return perPage > 0 && total > perPage;
    }

    private void placeIf(
            boolean condition,
            GuiContainer<Player, ItemStack> container,
            SlotSpec spec,
            Supplier<GuiItem<Player, ItemStack>> supplier
    ) {
        if (!condition || spec == null) return;
        for (int slot : spec.resolve(cfg.rows())) setAtSlot(container, slot, supplier.get());
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

    private void paintDebug(
        GuiContainer<Player, ItemStack> container,
        int rows
    ) {
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
    }

}
