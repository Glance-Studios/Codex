package com.glance.codex.platform.paper.config.model;

import com.glance.codex.api.collectable.config.model.ItemConfig;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import com.glance.codex.utils.item.LoreMergeMode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Data
@Accessors(fluent = true)
public class ItemEntry implements ConfigSerializable, ItemConfig {

    @ConfigField(order = 1)
    private Material material = Material.EGG;

    @ConfigField(order = 2)
    private String displayName = "";

    @ConfigField(order = 3)
    private List<String> lore;

    @ConfigField(order = 4)
    private LoreMergeMode loreMergeMode;

    @ConfigField
    private boolean glint;

    // sub 1.20.5
    @ConfigField
    private Integer customModelData;

    @ConfigField
    private List<ItemFlag> flags;

    // todo
    @ConfigField
    private String skullTexture;

    // 1.20.5 + new custom model component system
    @ConfigField
    private Map<String, Object> itemComponents;

    @ConfigField
    private LineWrapOptions lineWrap;

    @Override
    public String rawDisplayName() {
        return this.displayName;
    }

    /*
    * ==============
    * Simple Builder
    * ==============
    */

    /** Create a new ItemEntry with the given material */
    public static @NotNull ItemEntry of(@NotNull Material material) {
        ItemEntry e = new ItemEntry();
        e.material = material;
        return e;
    }

    /** Create a new ItemEntry with material and display name */
    public static @NotNull ItemEntry of(@NotNull Material material, @NotNull String displayName) {
        return of(material).name(displayName);
    }

    /** Chainable setter for display name (MiniMessage or legacy allowed) */
    public @NotNull ItemEntry name(@NotNull String displayName) {
        this.displayName = displayName;
        return this;
    }

}
