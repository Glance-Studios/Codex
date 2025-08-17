package com.glance.codex.platform.paper.config.model;

import com.glance.codex.api.collectable.config.model.LineWrapConfig;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class LineWrapOptions implements ConfigSerializable, LineWrapConfig {

    /**
     * The max number of characters per lore line before wrapping
     * <p>
     * If set to Integer.MAX_VALUE or -1, no wrapping will occur
     */
    @ConfigField(order = 1)
    private final Integer maxLineLength;

    /**
     * Whether long words should be forcibly broken if they exceed the line length
     */
    @ConfigField(order = 2)
    private final boolean breakWords;

    public static final LineWrapOptions DISABLED = new LineWrapOptions(-1, false);

    public static LineWrapOptions from(LineWrapConfig cfg) {
        return new LineWrapOptions(cfg.maxLineLength(), cfg.breakWords());
    }

}
