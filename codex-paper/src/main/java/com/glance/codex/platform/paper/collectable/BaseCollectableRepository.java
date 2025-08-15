package com.glance.codex.platform.paper.collectable;

import com.glance.codex.api.collectable.Collectable;
import com.glance.codex.api.collectable.CollectableRepository;
import com.glance.codex.api.collectable.config.RepositoryConfig;
import com.glance.codex.api.text.PlaceholderService;
import com.glance.codex.platform.paper.config.model.ItemEntry;
import com.glance.codex.platform.paper.item.ItemBuilder;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Slf4j
public class BaseCollectableRepository implements CollectableRepository {

    private final PlaceholderService resolver;

    private final String namespace;
    private final ItemEntry icon;
    private final ItemEntry selectedIcon;
    private final Map<String, Collectable> entries;


    @Inject
    public BaseCollectableRepository(
        @Assisted final RepositoryConfig cfg,
        @Assisted final Map<String, Collectable> entries,
        @NotNull final PlaceholderService resolver
    ) {
        this.resolver = resolver;

        this.entries = entries;
        this.namespace = cfg.namespace();
        this.icon = cfg.icon();
        this.selectedIcon = cfg.selectedIcon();
    }

    @Override
    public @NotNull String namespace() {
        return namespace;
    }

    @Override
    public @NotNull Map<String, Collectable> entries() {
        return this.entries;
    }

    @Override
    public @NotNull ItemStack getRepoIcon(@Nullable OfflinePlayer player) {
        return ItemBuilder.fromConfig(this.icon, player, resolver).build();
    }

    @Override
    public @NotNull ItemStack getSelectedIcon(@Nullable OfflinePlayer player) {
        return ItemBuilder.fromConfig(this.selectedIcon, player, resolver).build();
    }

    @Override
    public @Nullable Collectable get(@NotNull NamespacedKey key) {
        if (!namespace.equals(key.namespace())) return null;
        return entries.get(key.getKey());
    }

}
