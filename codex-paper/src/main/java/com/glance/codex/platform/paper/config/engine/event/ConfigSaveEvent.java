package com.glance.codex.platform.paper.config.engine.event;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ConfigSaveEvent extends ConfigEvent {

    private static final HandlerList handlers = new HandlerList();

    public ConfigSaveEvent(
        @NotNull Class<?> configClass,
        @NotNull Config.Handler instance
    ) {
        super(configClass, instance);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
