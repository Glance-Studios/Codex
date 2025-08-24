package com.glance.codex.platform.paper.config.engine.event;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.reload.ReloadResult;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ConfigInstanceReloadEvent extends ConfigEvent {

    private static final HandlerList handlers = new HandlerList();

    @Getter
    private final ReloadResult result;

    public ConfigInstanceReloadEvent(
            @NotNull Class<?> configClass,
            @NotNull Config.Handler instance,
            @NotNull ReloadResult result
            ) {
        super(configClass, instance);
        this.result = result;
    }

    /**
     * @return {@code true} if the config file contents (backing section) were reloaded and changed
     */
    public boolean causedSectionChanges() {
        return result.sectionChanged();
    }

    /**
     * @return {@code true} if reloading caused updates to the instances fields
     */
    public boolean causedFieldsChanges() {
        return result.fieldsChanged();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
