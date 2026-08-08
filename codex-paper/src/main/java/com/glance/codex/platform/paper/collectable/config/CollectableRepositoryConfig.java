package com.glance.codex.platform.paper.collectable.config;

import com.glance.codex.api.collectable.config.RepositoryConfig;
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

    @ConfigPath("plain_display_name")
    private String plainDisplayName;

    @ConfigPath("icon")
    private ItemEntry icon = ItemEntry.of(Material.EGG);

    @ConfigPath("selected_icon")
    private ItemEntry selectedIcon = ItemEntry.of(Material.EGG).name("Selected!");

    @ConfigPath("show_when_locked")
    private boolean showWhenLocked = true;

    @ConfigPath(value = "player_message_on_discover", comments = {
        "Chat message sent to the player the first time they discover an entry here.",
        "MiniMessage, with placeholders such as {collectable_name_formatted}, {player}, {repo_name_formatted}.",
        "An entry's own 'playerMessageOnDiscover' overrides this."
    })
    private String playerMessageOnDiscover;

    @ConfigPath(value = "global_message_on_discover", comments = {
        "As above, but broadcast to the whole server on a first discovery."
    })
    private String globalMessageOnDiscover;

    @ConfigPath(value = "player_message_on_replay", comments = {
        "Optional. Sent when an already unlocked entry is opened again.",
        "Left unset, replays stay silent so the discover message only fires the first time."
    })
    private String playerMessageOnReplay;

    @ConfigPath(value = "global_message_on_replay")
    private String globalMessageOnReplay;

    @ConfigPath("entries")
    private ConfigurationSection rawEntries;

}
