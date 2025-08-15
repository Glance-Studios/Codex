package com.glance.codex.platform.paper.collectable;

import com.glance.codex.platform.paper.CodexPlugin;
import com.glance.codex.api.collectable.Collectable;
import com.glance.codex.api.collectable.CollectableMeta;
import com.glance.codex.api.collectable.Discoverable;
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

    protected CollectableMeta meta;

    @ConfigField
    protected String type;

    @ConfigField
    protected String displayName;

    @ConfigField
    protected String rawDisplayName;

    @ConfigField
    protected boolean showWhenLocked;

    @ConfigField
    protected boolean allowReplay;

    @ConfigField
    protected ItemEntry unlockedIcon;

    @ConfigField
    protected ItemEntry lockedIcon;

    @ConfigField
    protected CommandEntry commandsOnDiscover;

    @ConfigField
    protected CommandEntry commandsOnReplay;

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
    public @NotNull ItemStack iconUnlocked(@Nullable OfflinePlayer player) {
        return ItemBuilder.fromConfig(unlockedIcon, player, CodexPlugin.getInstance().placeholderService()).build();
    }

    @Override
    public @NotNull ItemStack iconLocked(@Nullable OfflinePlayer player) {
        return ItemBuilder.fromConfig(lockedIcon, player, CodexPlugin.getInstance().placeholderService()).build();
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
    public void setMeta(@NotNull CollectableMeta meta) {
        this.meta = meta;
    }

    @Override
    public @Nullable CollectableMeta getMeta() {
        return this.meta;
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
