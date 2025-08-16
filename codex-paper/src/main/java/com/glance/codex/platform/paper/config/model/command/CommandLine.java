package com.glance.codex.platform.paper.config.model.command;

import com.glance.codex.api.collectable.config.model.command.CommandInfo;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class CommandLine implements ConfigSerializable, CommandInfo {

    @ConfigField(order = 1)
    private CommandInfo.Target runAs = CommandInfo.Target.CONSOLE;

    @ConfigField(order = 2)
    private String command;

}
