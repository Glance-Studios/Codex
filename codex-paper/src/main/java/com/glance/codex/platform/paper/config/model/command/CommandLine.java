package com.glance.codex.platform.paper.config.model.command;

import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import lombok.Data;

@Data
public class CommandLine implements ConfigSerializable {

    @ConfigField(order = 1)
    private Target runAs = Target.CONSOLE;

    @ConfigField(order = 2)
    private String command;

    public enum Target {
        CONSOLE,
        PLAYER
    }

}
