package com.glance.codex.platform.paper.api.collectable.base;

import com.glance.codex.platform.paper.api.collectable.Collectable;
import com.glance.codex.platform.paper.api.collectable.CollectableRepository;
import com.glance.codex.platform.paper.api.collectable.config.RepositoryConfig;
import com.glance.codex.platform.paper.api.text.PlaceholderService;
import com.glance.codex.platform.paper.command.executor.CommandExecutorService;
import com.glance.codex.platform.paper.config.model.ItemEntry;
import com.glance.codex.platform.paper.item.ItemBuilder;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

// todo factory
public class BaseCollectableRepository implements CollectableRepository {

    private final String namespace;
    private final ItemEntry icon;
    private final ItemEntry selectedIcon;
    private final Map<String, Collectable> entries;
    private final CommandExecutorService commandExecutor;
    private final PlaceholderService resolver;

    @Inject
    public BaseCollectableRepository(
        @Assisted final RepositoryConfig<?> cfg,
        @NotNull final CommandExecutorService commandExecutor,
        @NotNull final PlaceholderService resolver
    ) {
        this.namespace = cfg.namespace();
        this.entries = new LinkedHashMap<>(cfg.entries());
        this.commandExecutor = commandExecutor;
        this.resolver = resolver;
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
        return null;
    }

    // todo unlock/replay here? To trigger the executor?


}
