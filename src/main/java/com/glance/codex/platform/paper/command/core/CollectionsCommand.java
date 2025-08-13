package com.glance.codex.platform.paper.command.core;

import com.glance.codex.platform.paper.api.collectable.CollectableManager;
import com.glance.codex.platform.paper.api.collectable.CollectableRepository;
import com.glance.codex.platform.paper.command.engine.CommandHandler;
import com.glance.codex.platform.paper.menu.CollectablesMenu;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
@Singleton
@AutoService(CommandHandler.class)
public class CollectionsCommand implements CommandHandler {

    private final Plugin plugin;
    private final CollectableManager manager;
    private final CollectablesMenu menu;

    @Inject
    public CollectionsCommand(
        @NotNull Plugin plugin,
        @NotNull CollectableManager manager,
        @NotNull CollectablesMenu menu
    ) {
        this.manager = manager;
        this.menu = menu;
        this.plugin = plugin;
    }

    @Suggestions("namespaces")
    public List<String> suggestNamespaces(
        final CommandContext<CommandSender> ctx,
        final String input
    ) {
        final String prefix = input == null ? "" : input;
        return manager.getRepositories()
                .stream()
                .map(CollectableRepository::namespace)
                .filter(ns -> ns.regionMatches(true, 0, prefix, 0, prefix.length()))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Suggestions("entries")
    public List<String> suggestEntries(
        final CommandContext<CommandSender> ctx,
        final String input
    ) {
        final String ns = ctx.getOrDefault("namespace", "");
        if (ns.isEmpty()) return List.of();

        final CollectableRepository repo = manager.getRepo(ns);
        if (repo == null) return List.of();

        final String prefix = input == null ? "" : input;
        return repo.entries().keySet().stream()
                .filter(id -> id.regionMatches(true, 0, prefix, 0, prefix.length()))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Command("collectables unlock <player> <namespace> <id>")
    @Permission("collectables.admin")
    public void unlock(
            @NotNull CommandSender sender,
            @Argument("player") Player target,
            @Argument(value = "namespace", suggestions = "namespaces") String namespace,
            @Argument(value = "id", suggestions = "entries") String id
    ) {
       final NamespacedKey key = NamespacedKey.fromString(namespace + ":" + id);
       if (key == null) {
           sender.sendMessage("Invalid key + " + namespace + ":" + id);
           return;
       }

       manager.unlock(target, key).thenAccept(success -> {
           if (success) {
               sender.sendMessage("Unlocked " + key.asString() + " for " + target.getName());
           } else {
               sender.sendMessage(target.getName() + " already unlocked (and did not replay) " + key.asString());
           }
       }).exceptionally(ex -> {
           sender.sendMessage("Unlock failed: " + ex.getMessage());
           log.error("Failed unlock for target player '{}' using key '{}' due to: ",
                   target.getName(), key.asString(), ex);
          return null;
       });

    }

    @Command("collectables")
    @Permission("collectables.menu")
    public void openMenu(@NotNull Player sender) {
        this.menu.open(sender, false);
    }

}
