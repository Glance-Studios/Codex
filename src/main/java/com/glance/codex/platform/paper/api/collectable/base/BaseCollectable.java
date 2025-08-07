package com.glance.codex.platform.paper.api.collectable.base;

import com.glance.codex.platform.paper.CodexPlugin;
import com.glance.codex.platform.paper.api.collectable.Collectable;
import com.glance.codex.platform.paper.api.collectable.Discoverable;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import com.glance.codex.platform.paper.config.model.ItemEntry;
import com.glance.codex.platform.paper.config.model.command.CommandEntry;
import com.glance.codex.platform.paper.item.ItemBuilder;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
public class BaseCollectable implements Collectable, Discoverable, ConfigSerializable {

    @ConfigField
    private String key;

    @ConfigField
    private String displayName;

    @ConfigField
    private String rawDisplayName;

    @ConfigField
    private boolean showWhenLocked;

    @ConfigField
    private boolean allowReplay;

    @ConfigField
    private ItemEntry iconEntry;

    @ConfigField
    private CommandEntry commandsOnDiscover;

    @ConfigField
    private CommandEntry commandsOnReplay;

    @Override
    public @NotNull String namespace() {
        return key;
    }

    @Override
    public @NotNull Component displayName() {
        String resolved = CodexPlugin
                .getInstance()
                .placeholderService()
                .apply(displayName, null, Map.of("collectable", key));
        return MiniMessage.miniMessage().deserialize(resolved);
    }

    @Override
    public @NotNull String rawDisplayName() {
        return CodexPlugin
                .getInstance()
                .placeholderService()
                .apply(displayName, null, Map.of("collectable", key));
    }

    @Override
    public @NotNull ItemStack icon() {
        return ItemBuilder.fromConfig(iconEntry, null, CodexPlugin.getInstance().placeholderService()).build();
    }

    @Override
    public boolean showWhenLocked() {
        return showWhenLocked;
    }

    @Override
    public boolean allowReplay() {
        return allowReplay;
    }

    @Override
    public void onDiscover(@NotNull Player player) {
        // default no-op
    }

    @Override
    public void onReplay(@NotNull Player player) {
        // default no-op
    }

}
