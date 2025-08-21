package com.glance.codex.platform.paper.collectable;

import com.glance.codex.api.collectable.Collectable;
import com.glance.codex.api.collectable.CollectableRepository;
import com.glance.codex.api.collectable.config.RepositoryConfig;
import com.glance.codex.api.collectable.config.model.ItemConfig;
import com.glance.codex.api.text.PlaceholderService;
import com.glance.codex.platform.paper.config.model.ItemEntry;
import com.glance.codex.platform.paper.item.ItemBuilder;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Slf4j
public class DefaultCollectableRepository implements CollectableRepository {

    private final PlaceholderService resolver;

    private final String namespace;
    private final ItemConfig icon;
    private final ItemConfig selectedIcon;
    private final Map<String, Collectable> entries;

    private final String displayName;
    private final String plainDisplayName;


    @Inject
    public DefaultCollectableRepository(
        @Assisted final RepositoryConfig cfg,
        @Assisted final Map<String, Collectable> entries,
        @NotNull final PlaceholderService resolver
    ) {
        this.resolver = resolver;

        this.entries = entries;
        this.namespace = cfg.namespace();
        this.icon = cfg.icon();
        this.selectedIcon = cfg.selectedIcon();
        this.displayName = cfg.displayName();
        this.plainDisplayName = cfg.plainDisplayName();
    }

    @Override
    public @NotNull String namespace() {
        return namespace;
    }

    @Override
    public @NotNull Component displayName() {
        String resolved = resolver.apply(this.displayName, null);
        return MiniMessage.miniMessage().deserialize(resolved);
    }

    @Override
    public @NotNull String displayNameRaw() {
        return resolver.apply(this.displayName, null);
    }

    @Override
    public @NotNull String plainDisplayName() {
        return (plainDisplayName == null || plainDisplayName.isBlank())
                ? PlainTextComponentSerializer.plainText().serialize(displayName())
                : plainDisplayName;
    }

    @Override
    public @NotNull Map<String, Collectable> entries() {
        return this.entries;
    }

    @Override
    public @NotNull ItemStack getRepoIcon(@Nullable OfflinePlayer player) {
        ItemConfig iconCfg;
        if (icon.rawDisplayName() == null || icon.rawDisplayName().isBlank()) {
            iconCfg = ItemEntry.from(icon).name(displayName);
        } else iconCfg = icon;
        return ItemBuilder.fromConfig(iconCfg, player, resolver).build();
    }

    @Override
    public @NotNull ItemStack getSelectedIcon(@Nullable OfflinePlayer player) {
        ItemConfig iconCfg;
        if (selectedIcon.rawDisplayName() == null || selectedIcon.rawDisplayName().isBlank()) {
            iconCfg = ItemEntry.from(selectedIcon).name(displayName);
        } else iconCfg = selectedIcon;
        return ItemBuilder.fromConfig(iconCfg, player, resolver).build();
    }

    @Override
    public @Nullable Collectable get(@NotNull NamespacedKey key) {
        if (!namespace.equals(key.namespace())) return null;
        return entries.get(key.getKey());
    }

}
