package com.glance.codex.platform.paper.menu;

import com.glance.codex.platform.paper.api.collectable.Collectable;
import com.glance.codex.platform.paper.api.collectable.CollectableManager;
import com.glance.codex.platform.paper.api.collectable.CollectableRepository;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

/**
 * Handles building and displaying the {@code /collectables} menu to players
 * <p>
 * ---
 * </p>
 * <h3>Key Behaviors</h3>
 * <ul>
 *   <li>Shows all configured repositories in a selectable navigation area</li>
 *   <li>Displays visible entries (unlocked or {@code showWhenLocked=true}) with paging support</li>
 *   <li>Performs a two-pass render:
 *     <ul>
 *       <li>First pass: shows locked entries (optionally with a loading badge) if unlocked set is not cached</li>
 *       <li>Second pass: re-renders entries once unlocked IDs are loaded asynchronously</li>
 *     </ul>
 *   </li>
 *   <li>Supports a debug mode that visualizes slot regions and navigation controls</li>
 *   <li>Integrates with {@link PlaceholderService} for placeholder resolution in configured items</li>
 * </ul>
 *
 * <p><b>Threading note:</b> All GUI open calls and Triumph GUI interactions are performed
 * synchronously on the Bukkit main thread. Data fetches may run async but are scheduled back
 * to the main thread for UI updates</p>
 *
 * @author Cammy
 */
@Slf4j
@Singleton
public class CollectablesMenu {

    // TODO: add to config
    private final static Component LOADING_TEXT =
            Component.text(" (Loading…)", NamedTextColor.GRAY, TextDecoration.ITALIC);

    private final Plugin plugin;
    private final CollectableMenuConfig cfg;
    private final PlaceholderService placeholderService;
    private final CollectableManager collectableManager;

    private final MiniMessage MM = MiniMessage.miniMessage();

    @Inject
    public CollectablesMenu(
            @NotNull final Plugin plugin,
            @NotNull final CollectableMenuConfig cfg,
            @NotNull final PlaceholderService placeholderService,
            @NotNull final CollectableManager collectableManager
    ) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.placeholderService = placeholderService;
        this.collectableManager = collectableManager;
    }

    /**
     * Opens the collectables menu for a player
     *
     * @param player the player to show the menu to
     * @param debug if true, renders debug slot overlays instead of normal content
     */
    public void open(@NotNull Player player, boolean debug) {
        int rows = Math.max(1, Math.min(6, cfg.rows()));

        final List<CollectableRepository> repos = new ArrayList<>(collectableManager.getRepositories());
        if (repos.isEmpty()) {
            Gui.of(rows).title(MM.deserialize("<red>No collections configured>")).build().open(player);
            return;
        }

        final CollectableRepository initialRepo = repos.getFirst();
        final String initialNs = initialRepo.namespace();

        collectableManager.unlockedIds(player, initialNs).whenComplete((initialSet, ex) ->
            Bukkit.getScheduler().runTask(plugin, () -> {
               Set<String> ready = (ex == null) ? initialSet : Collections.emptySet();
               Gui gui = buildGui(player, rows, repos, initialNs, ready, debug);
               gui.open(player);
            })
        );
    }

    /*
     * ===========
     * GUI Builder
     * ===========
     */

    /**
     * Builds the GUI object for the collectables menu
     *
     * @param player player viewing the menu
     * @param rows number of menu rows
     * @param repos list of repositories to display
     * @param initialNamespace namespace of initially selected repo
     * @param ready unlocked IDs for the initial repo
     * @param debug whether debug mode is active
     * @return the built {@link Gui} instance
     */
    private Gui buildGui(
        @NotNull Player player,
        int rows,
        List<CollectableRepository> repos,
        String initialNamespace,
        Set<String> ready,
        boolean debug
    ) {
        return Gui.of(rows)
            .title(MM.deserialize(cfg.title()))
            .component(component -> {
                CollectablesVM vm = CollectablesVM.remember(
                        component, plugin, collectableManager,
                        player, 0, initialNamespace, ready);

                component.render(container ->
                        renderContent(container, player, rows, repos, vm, debug));
            })
            .build();
    }

    /*
    * =============
    * Gui Rendering
    * =============
    */

    /**
     * Renders menu contents (repositories, entries, navigation) into the container
     */
    private void renderContent(
            GuiContainer<Player, ItemStack> container,
            Player player,
            int rows,
            List<CollectableRepository> repos,
            CollectablesVM vm,
            boolean debug
    ) {
        final int size = rows * 9;
        applyFiller(container, size, cfg.filler(), player);
        if (debug) {
            paintDebug(container, rows);
            return;
        }
        if (repos.isEmpty()) return;

        int idx = Math.max(0, Math.min(vm.repoIndex.get(), repos.size() - 1));
        CollectableRepository repo = repos.get(idx);
        String namespace = repo.namespace();

        // Repository Navigation
        paintRepos(container, player, repos, repo, vm.repoIndex, vm.repoPage);

        // Two-pass render if not cached
        boolean cached = vm.hasUnlocked(namespace);
        Set<String> unlocked = vm.unlocked(namespace);
        boolean showLoadingBadge = !cached && vm.isLoading(namespace); // TODO: and config flag

        paintEntries(container, player, repo, unlocked, vm.entryPage, showLoadingBadge, (entryId, collectable, slot) -> {
            // TODO: any click logic
        });

        // Fetch only when selecting a repo and it's not cached yet
        vm.ensureLoaded(namespace);
    }

    /**
     * Renders repository selector buttons and repo paging controls
     */
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

            // TODO: anything when repo not unlocked for player

            int finalI = i;
            final GuiItem<Player, ItemStack> btn = dev.triumphteam.gui.paper.builder.item.ItemBuilder
                    .from(icon).asGuiItem((v, ctx) -> repoIndex.update(old -> finalI));

            setAtSlot(container, slot, btn);
        }

        final boolean paging = needsPaging(repos.size(), perPage);
        final boolean showPrev = paging && page > 0;
        final boolean showNext = paging && page < maxPage;

        placeIf(showPrev, container, cfg.repoPrev(), () -> {
            final ItemStack prev = ItemBuilder.fromConfig(cfg.repoPrevItem(), player, placeholderService).build();
            return dev.triumphteam.gui.paper.builder.item.ItemBuilder.from(prev).asGuiItem((v, ctx) ->
                    repoPage.update(p -> Math.max(0, p - 1)));
        });

        placeIf(showNext, container, cfg.repoNext(), () -> {
            final ItemStack next = ItemBuilder.fromConfig(cfg.repoNextItem(), player, placeholderService).build();
            return dev.triumphteam.gui.paper.builder.item.ItemBuilder.from(next).asGuiItem((v, ctx) ->
                    repoPage.update(p -> Math.min(maxPage, p + 1)));
        });
    }

    /**
     * Renders collectable entry icons for the current repository
     */
    private void paintEntries(
            GuiContainer<Player, ItemStack> container,
            Player player,
            CollectableRepository repo,
            Set<String> unlockedIds,
            MutableState<Integer> entryPage,
            boolean loadingBadge,
            TriConsumer<String, Collectable, Integer> onClick
    ) {
        var slots = cfg.entrySlots().resolve(cfg.rows());
        if (slots.isEmpty()) return;

        List<Map.Entry<String, Collectable>> index = getVisibleEntries(repo, unlockedIds);

        int perPage = slots.size();
        int maxPage = computeMaxPage(index.size(), perPage);
        int page = Math.max(0, Math.min(entryPage.get(), maxPage));
        int start = page * perPage;
        int end = Math.min(start + perPage, index.size());

        for (int i = start; i < end; i++) {
            int slot = slots.get(i - start);
            var entry = index.get(i);
            final String entryId = entry.getKey();
            final Collectable c = entry.getValue();

            final boolean isUnlocked = unlockedIds != null && unlockedIds.contains(entryId);

            ItemStack iconItem = isUnlocked
                    ? c.iconUnlocked(player)
                    : c.iconLocked(player);

            if (!isUnlocked && unlockedIds == null && loadingBadge) {
                iconItem.editMeta(meta -> {
                    Component currentName = meta.displayName();
                    Component loadingName = (currentName == null)
                            ? LOADING_TEXT
                            : currentName.append(LOADING_TEXT);
                    meta.displayName(loadingName);
                });
            }

            GuiItem<Player, ItemStack> icon = dev.triumphteam.gui.paper.builder.item.ItemBuilder
                .from(iconItem)
                .asGuiItem((v, ctx) -> onClick.accept(entryId, c, slot));
            setAtSlot(container, slot, icon);
        }

        final boolean paging = needsPaging(index.size(), perPage);
        final boolean showPrev = paging && page > 0;
        final boolean showNext = paging && page < maxPage;

        placeIf(showPrev, container, cfg.entryPrev(), () -> {
            final ItemStack prev = ItemBuilder.fromConfig(cfg.entryPrevItem(), player, placeholderService).build();
            return dev.triumphteam.gui.paper.builder.item.ItemBuilder.from(prev).asGuiItem((v, ctx) ->
                    entryPage.update(p -> Math.max(0, p - 1)));
        });

        placeIf(showNext, container, cfg.entryNext(), () -> {
            final ItemStack next = ItemBuilder.fromConfig(cfg.entryNextItem(), player, placeholderService).build();
            return dev.triumphteam.gui.paper.builder.item.ItemBuilder.from(next).asGuiItem((v, ctx) ->
                    entryPage.update(p -> Math.min(maxPage, p + 1)));
        });
    }

    /**
     * Filters repository entries to those visible to the player
     * <p>
     * Includes unlocked entries and locked entries with {@code showWhenLocked=true}
     *
     * @return list of visible entries
     */
    private List<Map.Entry<String, Collectable>> getVisibleEntries(
        CollectableRepository repo,
        @Nullable Set<String> unlocked
    ) {
        Map<String, Collectable> all = repo.entries();

        // Filter first: unlocked OR showWhenLocked
        List<Map.Entry<String, Collectable>> visible = new ArrayList<>(all.size());
        boolean haveUnlocked = (unlocked != null && !unlocked.isEmpty());
        for (var e : all.entrySet()) {
            String id = e.getKey();
            Collectable c = e.getValue();
            boolean isUnlocked = haveUnlocked && unlocked.contains(id);
            if (isUnlocked || c.showWhenLocked()) {
                visible.add(e);
            }
        }

        visible.sort(Comparator.comparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER));
        return visible;
    }

    /**
     * Sets an item in a container slot using row/column addressing
     */
    private void setAtSlot(
        GuiContainer<Player, ItemStack> container,
        int slot,
        GuiItem<Player, ItemStack> item
    ) {
        final int row = (slot / 9) + 1;
        final int col = (slot % 9) + 1;
        container.setItem(row, col, item);
    }

    /**
     * Places an item in the given slot specification if the condition is true
     */
    private void placeIf(
            boolean condition,
            GuiContainer<Player, ItemStack> container,
            SlotSpec spec,
            Supplier<GuiItem<Player, ItemStack>> supplier
    ) {
        if (!condition || spec == null) return;
        for (int slot : spec.resolve(cfg.rows())) setAtSlot(container, slot, supplier.get());
    }

    /**
     * Applies a filler item to all slots in the menu
     */
    private void applyFiller(
            GuiContainer<Player, ItemStack> container,
            int size,
            ItemEntry fillerEntry,
            Player player
    ) {
        ItemStack fillerItem = ItemBuilder.fromConfig(fillerEntry, player, placeholderService).build();
        GuiItem<Player, ItemStack> filler = dev.triumphteam.gui.paper.builder.item.ItemBuilder
                .from(fillerItem).asGuiItem((p, ctx) -> {});

        for (int slot = 0; slot < size; slot++) {
            setAtSlot(container, slot, filler);
        }
    }

    /**
     * Paints all slots defined in a {@link SlotSpec} with a given item
     */
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

    /*
    * ==========
    * Pagination
    * ==========
    */

    /**
     * Calculates the maximum zero-based page index for a given total and per-page size
     */
    private static int computeMaxPage(int total, int perPage) {
        if (perPage <= 0 || total <= 0) return 0;
        return Math.max(0, (total - 1) / perPage);
    }

    /**
     * Checks if paging controls are needed based on total items and per-page size
     */
    private static boolean needsPaging(int total, int perPage) {
        return perPage > 0 && total > perPage;
    }

    /*
    * Debug
    */

    /**
     * Paints debug overlays showing configured slot regions for repos, entries, and navigation
     */
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
