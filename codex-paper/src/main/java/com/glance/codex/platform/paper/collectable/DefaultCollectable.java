package com.glance.codex.platform.paper.collectable;

import com.glance.codex.api.collectable.base.PlayerCollectable;
import com.glance.codex.api.text.PlaceholderService;
import com.glance.codex.platform.paper.CodexPlugin;
import com.glance.codex.api.collectable.CollectableMeta;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import com.glance.codex.platform.paper.config.model.ItemEntry;
import com.glance.codex.platform.paper.config.model.command.CommandEntry;
import com.glance.codex.platform.paper.item.ItemBuilder;
import com.google.inject.Inject;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Accessors(fluent = true)
public class DefaultCollectable extends PlayerCollectable implements ConfigSerializable {

    private final PlaceholderService placeholderService;

    @Inject
    public DefaultCollectable(
            @NotNull final PlaceholderService placeholderService
    ) {
        this.placeholderService = placeholderService;
    }

    protected CollectableMeta meta;

    @ConfigField
    protected String type;

    @ConfigField
    protected String displayName;

    @ConfigField
    protected String plainDisplayName;

    @ConfigField
    protected Boolean showWhenLocked;

    @ConfigField
    protected Boolean allowReplay;

    @ConfigField
    protected Boolean trackReplays;

    @ConfigField
    protected Boolean replayOnClick;

    @ConfigField
    protected ItemEntry unlockedIcon;

    @ConfigField
    protected ItemEntry lockedIcon;

    @ConfigField
    protected CommandEntry commandsOnDiscover;

    @ConfigField
    protected CommandEntry commandsOnReplay;

    @ConfigField
    protected String playerMessageOnDiscover;

    @ConfigField
    protected String playerMessageOnReplay;

    @ConfigField
    protected String globalMessageOnDiscover;

    @ConfigField
    protected String globalMessageOnReplay;

    @ConfigField
    protected CommandEntry commandsOnMenuLeftClick;

    @ConfigField
    protected CommandEntry commandsOnMenuRightClick;

    @ConfigField
    protected CommandEntry commandsOnMenuShiftClick;

    /* Replay Defaults */

    @Override
    public boolean showWhenLocked() {
        return (showWhenLocked == null || showWhenLocked);
    }

    @Override
    public boolean allowReplay() {
        return (allowReplay == null || allowReplay);
    }

    @Override
    public boolean trackReplays() {
        return Boolean.TRUE.equals(trackReplays);
    }

    @Override
    public boolean replayOnClick() {
        return (replayOnClick == null || replayOnClick);
    }

    /* Messages */

    @Override public String playerMessageOnReplay() {
        return playerMessageOnReplay != null ? playerMessageOnReplay : playerMessageOnDiscover;
    }

    @Override public String globalMessageOnReplay() {
        return globalMessageOnReplay != null ? globalMessageOnReplay : globalMessageOnDiscover;
    }

    /* Display Name */

    @Override
    public @NotNull Component displayName() {
        String resolved = placeholderService.apply(displayName, null);
        return MiniMessage.miniMessage().deserialize(resolved);
    }

    @Override
    public @NotNull String rawDisplayName() {
        return placeholderService.apply(displayName, null);
    }

    @Override
    public @NotNull String plainDisplayName() {
        if (plainDisplayName == null || plainDisplayName.isBlank()) {
            plainDisplayName = PlainTextComponentSerializer.plainText().serialize(displayName());
        }
        return placeholderService.apply(plainDisplayName, null);
    }

    /* Commands */

    @Override
    public CommandEntry commandsOnReplay() {
        return (commandsOnReplay == null)
                ? commandsOnDiscover
                : commandsOnReplay;
    }

    @Override
    public @Nullable CommandEntry commandsOnMenuRightClick() {
        return (commandsOnMenuRightClick == null)
                ? commandsOnMenuLeftClick
                : commandsOnMenuRightClick;
    }

    @Override
    public @Nullable CommandEntry commandsOnMenuShiftClick() {
        return (commandsOnMenuShiftClick == null)
                ? commandsOnMenuLeftClick
                : commandsOnMenuShiftClick;
    }

    /* Icon */

    @Override
    public @NotNull ItemStack iconUnlocked(@Nullable OfflinePlayer player) {
        if (unlockedIcon.displayName() == null || unlockedIcon.displayName().isBlank()) {
            unlockedIcon.displayName(displayName);
        }
        return ItemBuilder.fromConfig(unlockedIcon, player, CodexPlugin.getInstance().placeholderService()).build();
    }

    @Override
    public @NotNull ItemStack iconLocked(@Nullable OfflinePlayer player) {
        if (lockedIcon.displayName() == null || lockedIcon.displayName().isBlank()) {
            lockedIcon.displayName("<dark_gray>???");
        }
        return ItemBuilder.fromConfig(lockedIcon, player, CodexPlugin.getInstance().placeholderService()).build();
    }

    @Override
    public void setMeta(@NotNull CollectableMeta meta) {
        this.meta = meta;
    }

    @Override
    public @Nullable CollectableMeta getMeta() {
        return this.meta;
    }

}
