package com.glance.codex.platform.paper.command.core;

import com.glance.codex.api.collectable.CollectableRepository;
import com.glance.codex.bootstrap.GuiceServiceLoader;
import com.glance.codex.platform.paper.command.engine.CommandHandler;
import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Singleton
@AutoService(CommandHandler.class)
public class ReloadCommand implements CommandHandler {

    private final Injector injector;

    // key -> handler class (case-insensitive keys)
    private final Map<String, Class<? extends Config.Handler>> handlerIndex =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    @Inject
    public ReloadCommand(
            @NotNull final Plugin plugin,
            @NotNull final Injector injector
    ) {
        this.injector = injector;

        for (var cfgClass : GuiceServiceLoader.load(Config.Handler.class, plugin.getClass().getClassLoader())) {
            String key = cfgClass.getSimpleName();
            handlerIndex.put(key, cfgClass);
        }
    }

    @Suggestions("configNames")
    public List<String> suggestNamespaces(
            final CommandContext<CommandSender> ctx,
            final String input
    ) {
        final String prefix = input == null ? "" : input;
        return handlerIndex.keySet().stream()
                .filter(k -> k.regionMatches(true, 0, prefix, 0, prefix.length()))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Command("collectables reload")
    public void reloadAll(
        @NotNull CommandSender sender
    ) {
        if (sender instanceof Player p) {
            p.getWorld().spawn(p.getLocation(), Slime.class);
        }

        for (var entry : handlerIndex.entrySet()) {
            Config.Handler config = this.injector.getInstance(entry.getValue());
            config.reload();
        }
    }

    @Command("collectables reload <key>")
    public void reloadOne(
        @NotNull CommandSender sender,
        @Argument(value = "key", suggestions = "configNames") String key
    ) {
        Class<? extends Config.Handler> type = handlerIndex.get(key);
        if (type == null) {
            sender.sendMessage("Unknown config key: " + key);
            return;
        }

        Config.Handler config = this.injector.getInstance(type);
        config.reload();
    }


}
