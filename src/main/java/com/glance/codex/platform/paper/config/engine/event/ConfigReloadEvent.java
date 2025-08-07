package com.glance.codex.platform.paper.config.engine.event;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ConfigReloadEvent extends ConfigEvent {

    private static final HandlerList handlers = new HandlerList();

    private final boolean sectionChanged;
    private final boolean fieldsChanged;

    public ConfigReloadEvent(
        @NotNull Class<?> configClass,
        @NotNull Config.Handler instance,
        boolean sectionChanged,
        boolean fieldsChanged
    ) {
        super(configClass, instance);
        this.sectionChanged = sectionChanged;
        this.fieldsChanged = fieldsChanged;
    }

    /**
     * @return {@code true} if the config file contents (backing section) were reloaded and changed
     */
    public boolean causedSectionChanges() {
        return sectionChanged;
    }

    /**
     * @return {@code true} if reloading caused updates to the instances fields
     */
    public boolean causedFieldsChanges() {
        return fieldsChanged;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
