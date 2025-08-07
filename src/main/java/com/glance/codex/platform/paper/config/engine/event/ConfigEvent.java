package com.glance.codex.platform.paper.config.engine.event;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import lombok.Getter;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class ConfigEvent extends Event {

    private final Class<?> configClass;
    private final Config.Handler instance;

    public ConfigEvent(
        @NotNull final Class<?> configClass,
        @NotNull final Config.Handler instance
    ) {
        this.configClass = configClass;
        this.instance = instance;
    }

}
