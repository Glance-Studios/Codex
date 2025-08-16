package com.glance.codex.api.collectable.config.model.command;

public interface CommandInfo {

    default Target runAs() {
        return Target.CONSOLE;
    }

    String command();

    enum Target {
        CONSOLE,
        PLAYER
    }

}
