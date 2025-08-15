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

    @ConfigPath("title") private String title = "<#5306bf>Collections";
    @ConfigPath("rows") private int rows = 6;

    // Entry grid
    @ConfigPath("entry.slots") private SlotSpec entrySlots= SlotSpec.of("rows:1-4");
    @ConfigPath("entry.nav.prev") private SlotSpec entryPrev = SlotSpec.of("row:5 col:1");
    @ConfigPath("entry.nav.next") private SlotSpec entryNext = SlotSpec.of("row:5 col:9");

    // Repository strip/grid + its nav
    @ConfigPath("repo.slots") private SlotSpec repoSlots = SlotSpec.of("row:6 cols:2-8");
    @ConfigPath("repo.nav.prev") private SlotSpec repoPrev = SlotSpec.of("row:6 col:1");
    @ConfigPath("repo.nav.next") private SlotSpec repoNext = SlotSpec.of("row:6 col:9");

    // Filler
    @ConfigPath("items.filler") private ItemEntry filler = ItemEntry
            .of(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name("<gray>Filler");

    // Buttons - todo
    @ConfigPath("items.repo.next") private ItemEntry repoNextItem;
    @ConfigPath("items.repo.prev") private ItemEntry repoPrevItem;
    @ConfigPath("items.entry.next") private ItemEntry entryNextItem;
    @ConfigPath("items.entry.prev") private ItemEntry entryPrevItem;

}
