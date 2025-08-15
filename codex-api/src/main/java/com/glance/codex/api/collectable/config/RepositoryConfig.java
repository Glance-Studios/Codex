package com.glance.codex.api.collectable.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RepositoryConfig {
    boolean enabled();
    @NotNull
    String namespace();

    @NotNull
    ItemEntry icon();

    @NotNull
    ItemEntry selectedIcon();

    /**
     * Raw node per entry
     * <p>
     * Keys are entry IDs; values are arbitrary maps
     */
    @Nullable
    ConfigurationSection rawEntries();
}
