package com.glance.codex.platform.paper.menu.config;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigPath;
import com.glance.codex.platform.paper.config.model.ItemEntry;
import com.glance.codex.platform.paper.menu.config.codec.SlotSpec;
import com.google.auto.service.AutoService;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.Material;

@Data
@Accessors(fluent = true)
@Config(path = "menus/collectables")
@AutoService(Config.Handler.class)
public class CollectableMenuConfig implements Config.Handler {

    @ConfigPath("title") private String title = "<gold>Collections</gold>";
    @ConfigPath("rows") private int rows = 6;

    // Entry grid
    @ConfigPath("entry.slots") private SlotSpec entrySlots = SlotSpec.of("9-34");
    @ConfigPath("entry.nav.prev") private SlotSpec entryPrev = SlotSpec.of("row:1 col:3");
    @ConfigPath("entry.nav.next") private SlotSpec entryNext = SlotSpec.of("row:1 col:7");

    // Repository strip/grid + its nav
    @ConfigPath("repo.slots") private SlotSpec repoSlots = SlotSpec.of("2-6");  // e.g., top row strip
    @ConfigPath("repo.nav.prev") private SlotSpec repoPrev = SlotSpec.of("row:6 col:0");
    @ConfigPath("repo.nav.next") private SlotSpec repoNext = SlotSpec.of("row:6 col:8");

    // Filler
    @ConfigPath("items.filler") private ItemEntry filler = ItemEntry
            .of(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name("<gray>Filler");

    // Buttons - todo
    @ConfigPath("items.repo.next") private ItemEntry repoNextItem;
    @ConfigPath("items.repo.prev") private ItemEntry repoPrevItem;
    @ConfigPath("items.entry.next") private ItemEntry entryNextItem;
    @ConfigPath("items.entry.prev") private ItemEntry entryPrevItem;

}
