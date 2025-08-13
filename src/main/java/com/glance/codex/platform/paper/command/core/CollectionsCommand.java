package com.glance.codex.platform.paper.command.core;

import com.glance.codex.platform.paper.api.collectable.CollectableManager;
import com.glance.codex.platform.paper.api.data.storage.CollectableStorage;
import com.glance.codex.platform.paper.command.engine.CommandHandler;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.annotations.Command;
import org.jetbrains.annotations.NotNull;

@Slf4j
@Singleton
@AutoService(CommandHandler.class)
public class CollectionsCommand implements CommandHandler {

    private final Plugin plugin;
    private final CollectableManager manager;

    @Inject
    public CollectionsCommand(
        @NotNull Plugin plugin,
        @NotNull CollectableManager manager
    ) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @Command("unlock")
    public void unlock() {
        // TODO
    }

}
