package com.glance.codex.platform.paper.config.engine.event;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ConfigPushEvent extends ConfigEvent {

    private static final HandlerList handlers = new HandlerList();

    private final boolean changed;

    public ConfigPushEvent(
        @NotNull Class<?> configClass,
        @NotNull Config.Handler instance,
        boolean changed
    ) {
        super(configClass, instance);
        this.changed = changed;
    }

    /**
     * @return {@code true} if pushing the current config instance to its backing
     * {@link ConfigurationSection} resulted in changes to the section
     * <p>
     * This indicates that the instance had new or modified values not yet present in the section cache
     */
    public boolean hasChanged() {
        return this.changed;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
