package com.glance.codex.platform.paper.config.engine.event;

import com.glance.codex.platform.paper.config.engine.ConfigController;
import com.glance.codex.platform.paper.config.engine.annotation.Config;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ConfigLoadEvent extends ConfigEvent {

    private static final HandlerList handlers = new HandlerList();

    private final boolean changed;

    public ConfigLoadEvent(
        @NotNull Class<?> configClass,
        @NotNull Config.Handler instance,
        boolean changed
    ) {
        super(configClass, instance);
        this.changed = changed;
    }

    /**
     * @return {@code true} if the config file was modified during loading
     * <p>
     * A return value of {@code true} typically means that {@link ConfigController} updated the
     * loaded {@link ConfigurationSection} in memory, and then wrote it to disk
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
