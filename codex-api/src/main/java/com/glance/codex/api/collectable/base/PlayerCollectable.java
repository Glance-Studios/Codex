package com.glance.codex.api.collectable.base;

import com.glance.codex.api.collectable.Collectable;
import com.glance.codex.api.collectable.Discoverable;
import com.glance.codex.api.collectable.config.model.command.CommandConfig;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class PlayerCollectable implements Collectable, Discoverable {

    public String globalMessageOnDiscover() {
        return "";
    }

    public String globalMessageOnReplay() {
        return globalMessageOnDiscover();
    }

    public String playerMessageOnDiscover() {
        return "";
    }

    public String playerMessageOnReplay() {
        return playerMessageOnDiscover();
    }

    public CommandConfig commandsOnDiscover() {
        return new CommandConfig(){};
    }

    public CommandConfig commandsOnReplay() {
        return commandsOnDiscover();
    }

    @Override
    public void onDiscover(@NotNull Player player) {
        // default no-op
    }

    @Override
    public void onReplay(@NotNull Player player) {
        // default no-op
    }

}
