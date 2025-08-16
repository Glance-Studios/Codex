package com.glance.codex.platform.paper.command.executor;

import com.glance.codex.api.collectable.config.model.command.CommandConfig;
import com.glance.codex.api.collectable.config.model.command.CommandInfo;
import com.glance.codex.api.text.PlaceholderService;
import com.glance.codex.platform.paper.config.model.command.CommandEntry;
import com.glance.codex.platform.paper.config.model.command.CommandLine;
import com.glance.codex.platform.paper.text.PlaceholderUtils;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
@AutoService(Manager.class)
public class CommandExecutorService implements Manager {

    private final PlaceholderService placeholderService;

    @Inject
    public CommandExecutorService(PlaceholderService placeholderService) {
        this.placeholderService = placeholderService;
    }

    /**
     * Executes all commands (per their {@link CommandLine#runAs()} modes) with no placeholders
     */
    public void execute(@NotNull CommandEntry command) {
        execute(command, null, Collections.emptyMap());
    }

    /**
     * Executes all commands (per their {@link CommandLine#runAs()} modes) with placeholder values
     *
     * @param placeholders key-value pairs to replace in the command
     */
    public void execute(@NotNull CommandEntry command, @Nullable Map<String, String> placeholders) {
        execute(command, null, placeholders);
    }

    /**
     * Executes all commands with the given player context (for PLAYER mode commands)
     *
     * @param player the target player
     */
    public void execute(@NotNull CommandEntry command, @NotNull Player player) {
        execute(command, player, null);
    }

    /**
     * Executes all commands with the given player and placeholder context
     * Commands will be run as console or player according to {@link CommandLine#runAs()}
     *
     * @param player the player context (used for PLAYER mode)
     * @param placeholders placeholders for command resolution
     */
    public void execute(
        @NotNull CommandConfig command,
        @Nullable Player player,
        @Nullable Map<String, String> placeholders
    ) {
        Map<String, String> full = (player != null)
                ? PlaceholderUtils.appendPlayerTags(player, placeholders)
                : (placeholders != null ? new HashMap<>(placeholders) : Collections.emptyMap());

        executeCommand(null, command, player, full);
    }

    /**
     * Executes all commands in this entry explicitly as the player (ignores per-line runAs)
     *
     * @param player the player to run commands as
     */
    public void executeAsPlayer(@NotNull CommandConfig command, @NotNull Player player) {
        executeAsPlayer(command, player, null);
    }

    /**
     * Executes all commands explicitly as the player, resolving placeholders
     *
     * @param player the player to run as
     * @param placeholders placeholder values
     */
    public void executeAsPlayer(
            @NotNull CommandConfig command,
            @NotNull Player player,
            @Nullable Map<String, String> placeholders
    ) {
        Map<String, String> full = PlaceholderUtils.appendPlayerTags(player, placeholders);
        executeCommand(CommandLine.Target.PLAYER, command, player, full);
    }

    /**
     * Executes all commands explicitly as the console, resolving placeholders
     *
     * @param placeholders optional placeholder values
     */
    public void executeAsConsole(
            @NotNull CommandConfig command,
            @Nullable Map<String, String> placeholders
    ) {
        executeCommand(CommandLine.Target.CONSOLE, command, null, placeholders);
    }

    private Map<String, String> resolvePlaceholders(Player player, Map<String, String> context) {
        Map<String, String> resolved = new HashMap<>();
        resolved.put("player", player.getName());
        if (context != null) resolved.putAll(context);
        return resolved;
    }

    private String replacePlaceholders(String command, Map<String, String> placeholders) {
        String result = command;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return result;
    }

    /**
     * Internal helper to resolve and dispatch commands using the specified run mode
     *
     * @param forceMode if nonnull, overrides each line’s {@link CommandInfo.Target}
     * @param entry the {@link CommandEntry} to run
     * @param player the player context (used for PLAYER target)
     * @param placeholders key-value placeholder map
     */
    private void executeCommand(
            @Nullable CommandInfo.Target forceMode,
            @NotNull CommandConfig<CommandLine> entry,
            @Nullable Player player,
            @Nullable Map<String, String> placeholders
    ) {
        if (!entry.enabled() || entry.commands().isEmpty()) return;

        for (CommandInfo line : entry.commands()) {
            String resolved = placeholderService.apply(line.command(), player, placeholders);

            CommandLine.Target mode = (forceMode != null) ? forceMode : line.runAs();
            CommandSender sender = switch (mode) {
                case CommandLine.Target.PLAYER -> player != null ? player : Bukkit.getConsoleSender();
                case CommandLine.Target.CONSOLE -> Bukkit.getConsoleSender();
            };

            Bukkit.dispatchCommand(sender, resolved);
        }
    }

}
