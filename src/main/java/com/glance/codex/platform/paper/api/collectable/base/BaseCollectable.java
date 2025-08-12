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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
public class BaseCollectable implements Collectable, Discoverable, ConfigSerializable {

    @ConfigField String type;

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
    public @NotNull Component displayName() {
        String resolved = CodexPlugin
                .getInstance()
                .placeholderService()
                .apply(displayName, null);
        return MiniMessage.miniMessage().deserialize(resolved);
    }

    @Override
    public @NotNull String rawDisplayName() {
        return CodexPlugin
                .getInstance()
                .placeholderService()
                .apply(displayName, null);
    }

    @Override
    public @NotNull ItemStack icon(@Nullable OfflinePlayer player) {
        return ItemBuilder.fromConfig(iconEntry, player, CodexPlugin.getInstance().placeholderService()).build();
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
