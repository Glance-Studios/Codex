package com.glance.codex.api.collectable.config.model;

import com.glance.codex.utils.item.LoreMergeMode;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.List;
import java.util.Map;

public interface ItemConfig {

    Material material();

    default String rawDisplayName() {
        return "";
    }

    default List<String> lore() {
        return List.of();
    }

    default LoreMergeMode loreMergeMode() {
        return LoreMergeMode.REPLACE;
    }

    default boolean glint() {
        return false;
    }

    default Integer customModelData() {
        return 0;
    }

    default List<ItemFlag> flags() {
        return List.of();
    }

    default Map<String, Object> itemComponents() {
        return Map.of();
    }

    default LineWrapConfig lineWrap() {
        return new LineWrapConfig(){};
    }

    default boolean hasModelData() {
        return customModelData() != null || (itemComponents() != null && !itemComponents().isEmpty());
    }


}
