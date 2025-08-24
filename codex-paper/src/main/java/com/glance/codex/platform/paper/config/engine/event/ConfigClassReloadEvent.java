package com.glance.codex.platform.paper.config.engine.event;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.reload.ReloadResult;
import com.glance.codex.platform.paper.config.engine.reload.ReloadSummary;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ConfigClassReloadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Class<?> configClass;
    private final List<Config.Handler> instances;
    private final ReloadSummary summary;

    public ConfigClassReloadEvent(
        @NotNull Class<?> configClass,
        @NotNull List<? extends Config.Handler> instances,
        @NotNull ReloadSummary summary
    ) {
        this.configClass = configClass;
        this.instances = List.copyOf(instances);
        this.summary = summary;
    }

    public @NotNull Class<?> configClass() { return configClass; }
    public @NotNull List<Config.Handler> instances() { return instances; }
    public @NotNull ReloadSummary reloadSummary() { return summary; }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() { return HANDLERS; }

}
