package com.glance.codex.platform.paper.config.model;

import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import lombok.Data;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ItemEntry implements ConfigSerializable {

    @ConfigField(order = 1)
    private Material material = Material.EGG;

    @ConfigField(order = 2)
    private String displayName = "";

    @ConfigField(order = 3)
    private List<String> lore = new ArrayList<>();

    @ConfigField(order = 4)
    private LoreMergeMode loreMergeMode;

    @ConfigField
    private boolean glint = false;

    // sub 1.20.5
    @ConfigField
    private Integer customModelData;

    @ConfigField
    private List<ItemFlag> flags = new ArrayList<>();

    // todo
    @ConfigField
    private String skullTexture;

    // 1.20.5 + new custom model component system
    @ConfigField
    private Map<String, Object> itemComponents;

    @ConfigField
    private LineWrapOptions lineWrap;

    public @NotNull LoreMergeMode mergeMode() {
        return this.loreMergeMode == null ? LoreMergeMode.REPLACE : loreMergeMode;
    }

    public boolean hasDisplay() {
        return (displayName != null && !displayName.isEmpty()) || (lore != null && !lore.isEmpty());
    }

    public boolean hasModelData() {
        return customModelData != null || (itemComponents != null && !itemComponents.isEmpty());
    }

    public enum LoreMergeMode {
        REPLACE,  // Replace all existing lore
        APPEND,   // Append new lines to existing lore
        PREPEND,  // Prepend new lines before existing lore
        IGNORE_IF_PRESENT // Only set lore if none exists
    }

}
