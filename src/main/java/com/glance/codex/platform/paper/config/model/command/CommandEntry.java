package com.glance.codex.platform.paper.config.model.command;

import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import lombok.Data;

import java.util.*;

/**
 * A configurable set of commands that can be executed when triggered by an event or condition
 * <p>
 * Each command can be executed as the console or player, and supports placeholder tokens like
 * {@code <player>}, {@code <rank>}, {@code <level>}, etc. Placeholders are resolved at runtime
 * <p>
 * This class supports:
 * <ul>
 *     <li>Per-command execution mode via {@link CommandLine#getRunAs()}</li>
 *     <li>Global forced execution as console or player</li>
 *     <li>Placeholder substitution</li>
 * </ul>
 *
 * Example:
 * <pre><code>
 * commands:
 *   - runAs: CONSOLE
 *     command: "lp user <player> parent add rank_<rank>"
 *   - runAs: PLAYER
 *     command: "say Woohoo!"
 * </code></pre>
 *
 * @author Cammy
 */
@Data
public class CommandEntry implements ConfigSerializable {

    /**
     * Whether this command entry is enabled
     * <p>
     * If false, no commands will be run even if invoked
     */
    @ConfigField(order = 1)
    private boolean enabled = true;

    /**
     * List of commands to run. Supports placeholders (e.g. {@code <player>})
     */
    @ConfigField(order = 2)
    private List<CommandLine> commands = new ArrayList<>();

}
