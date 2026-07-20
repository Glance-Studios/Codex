package com.glance.codex.platform.paper.config.model;

import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(fluent = true)
public class BookConfig implements ConfigSerializable {

    @ConfigField
    private boolean enabled = true;

    @ConfigField(order = 1)
    private String id = "";

    @ConfigField(order = 2)
    private String title = "Untitled";

    @ConfigField(order = 3)
    private String author = "Unknown";

    /**
     * Full title shown on the generated title page. Minecraft caps a written book's item
     * title at 32 chars, so {@link #title} is trimmed; the title page (page text) has no such
     * limit and uses this when set, falling back to {@link #title}.
     */
    @ConfigField
    private String displayTitle = "";

    @ConfigField
    private List<String> pages;

    @ConfigField
    private String content = "";

    @ConfigField
    private LineWrapOptions wrap;

    @ConfigField
    private int maxLinesPerPage = 14;

    @ConfigField
    private boolean collapseBlankLines = false;

    @ConfigField
    private boolean useMiniMessage = false;

    /**
     * Usable pixel width of a book page line. Text is wrapped to fit this width using
     * the vanilla font metrics so nothing overflows and gets clipped. Vanilla pages are
     * ~114px; the default leaves a small safety margin.
     */
    @ConfigField
    private int pageWidthPixels = 113;

    /**
     * If true, prepend a generated title page showing the book's title and author,
     * centered. Vanilla written books only show these on hover, never as a page.
     */
    @ConfigField
    private boolean titlePage = false;

}
