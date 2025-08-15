package com.glance.codex.platform.paper.collectable.config;

import com.glance.codex.platform.paper.api.collectable.config.RepositoryConfig;
import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigPath;
import com.glance.codex.platform.paper.config.model.ItemEntry;
import com.google.auto.service.AutoService;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

@Data
@Accessors(fluent = true)
@Config(path = "collectables/*", writeDefaults = false)
@AutoService(Config.Handler.class)
public class CollectableRepositoryConfig implements Config.Contract, RepositoryConfig {

    @ConfigPath("enabled")
    private boolean enabled = true;

    @ConfigPath("namespace")
    private String namespace = "";

    @ConfigPath("display_name")
    private String displayName = "";

    @ConfigPath("display_name_raw")
    private String displayNameRaw = "";

    @ConfigPath("icon")
    private ItemEntry icon = ItemEntry.of(Material.EGG);

    @ConfigPath("selected_icon")
    private ItemEntry selectedIcon = ItemEntry.of(Material.EGG).name("Selected!");

    @ConfigPath("show_when_locked")
    private boolean showWhenLocked = true;

    @ConfigPath("entries")
    private ConfigurationSection rawEntries;

}
