package com.glance.codex.api.collectable.config.model.command;

import java.util.List;

public interface CommandConfig<T extends CommandInfo> {

    default boolean enabled() {
        return true;
    }

    default List<T> commands() {
        return List.of();
    }

}
