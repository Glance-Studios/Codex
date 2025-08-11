package com.glance.codex.platform.paper.menu.config;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigPath;
import com.glance.codex.platform.paper.config.model.ItemEntry;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@Config(path = "menus/collectables")
public class CollectableMenuConfig implements Config.Handler {

    @ConfigPath("title") private String title = "<gold>Collections</gold>";
    @ConfigPath("rows") private int rows = 6;

    // Entry grid
    @ConfigPath("entry.slots") private String entrySlots = "10-34";
    @ConfigPath("entry.nav.prev") private String entryPrev = "row:6 col:3";
    @ConfigPath("entry.nav.next") private String entryNext = "row:6 col:7";

    // Repository strip/grid + its nav
    @ConfigPath("repo.slots") private String repoSlots = "2-6";     // e.g., top row strip
    @ConfigPath("repo.nav.prev") private String repoPrev = "0";     // left top corner
    @ConfigPath("repo.nav.next") private String repoNext = "8";     // right top corner

    // Filler
    @ConfigPath("filler.material") private String fillerMaterial = "BLACK_STAINED_GLASS_PANE";
    @ConfigPath("filler.name") private String fillerName = "<gray> ";

    // Buttons - todo
    @ConfigPath("items.repo.next") private ItemEntry repoNextItem;
    @ConfigPath("items.repo.prev") private ItemEntry repoPrevItem;
    @ConfigPath("items.entry.next") private ItemEntry entryNextItem;
    @ConfigPath("items.entry.prev") private ItemEntry entryPrevItem;

}
