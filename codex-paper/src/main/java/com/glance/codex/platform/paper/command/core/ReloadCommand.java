package com.glance.codex.platform.paper.command.core;

import com.glance.codex.platform.paper.command.engine.CommandHandler;
import com.glance.codex.platform.paper.config.engine.ConfigController;
import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.reload.ConfigReloader;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
@AutoService(CommandHandler.class)
public class ReloadCommand implements CommandHandler {

    private final ConfigReloader reloader;

    @Inject
    public ReloadCommand(
            @NotNull final Plugin plugin,
            @NotNull final ConfigReloader reloader
    ) {
        this.reloader = reloader;
    }

    @Suggestions("configNames")
    public List<String> suggestNamespaces(
            final CommandContext<CommandSender> ctx,
            final String input
    ) {
        final String prefix = input == null ? "" : input;

        return ConfigController.allInstances().stream()
                .map(Object::getClass)
                .distinct()
                .filter(this::isHotReloadableClass)
                .map(ConfigController::classKeyFor)
                .filter(k -> k.regionMatches(true, 0, prefix, 0, prefix.length()))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private boolean isHotReloadableClass(Class<?> cls) {
        Config meta = cls.getAnnotation(Config.class);
        return meta == null || meta.supportHotReload();
    }

    @Command("collectables reload")
    public void reloadAll(
        @NotNull CommandSender sender
    ) {
        this.reloader.reloadAll().thenAccept(sum ->
                sender.sendMessage(sum.prettyPrint()));
    }

    @Command("collectables reload <key>")
    public void reloadClass(
        @NotNull CommandSender sender,
        @Argument(value = "key", suggestions = "configNames") String key
    ) {
        Class<? extends Config.Handler> type = ConfigController.resolveClassByKey(key).orElse(null);
        if (type == null) {
            sender.sendMessage("Unknown config class: " + key);
            return;
        }

        sender.sendMessage("Reloading class " + key);
        this.reloader.reloadAllOf(type).thenAccept(sum ->
                sender.sendMessage(sum.prettyPrint()));
    }


}
