package com.glance.codex.api.event;

import com.glance.codex.api.collectable.CollectableAPI;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CodexReadyEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final CollectableAPI api;

    public CodexReadyEvent(CollectableAPI api) { this.api = api; }

    public CollectableAPI api() { return api; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
