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

}
