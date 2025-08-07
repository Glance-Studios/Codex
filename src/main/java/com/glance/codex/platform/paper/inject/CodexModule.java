package com.glance.codex.platform.paper.inject;

import com.glance.codex.platform.paper.api.text.PlaceholderService;
import com.glance.codex.platform.paper.text.DefaultPlaceholderService;
import com.glance.codex.platform.paper.text.PapiPlaceholderService;
import com.google.inject.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class CodexModule extends AbstractModule {

    private final Plugin plugin;

    public CodexModule(@NotNull final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // bind plugin instance
        this.bind(Plugin.class).toInstance(plugin);
        this.bind(JavaPlugin.class).toInstance((JavaPlugin) plugin);

        if (isPapiPresent()) {
            bind(PlaceholderService.class).to(PapiPlaceholderService.class).asEagerSingleton();
        } else {
            bind(PlaceholderService.class).to(DefaultPlaceholderService.class).asEagerSingleton();
        }
    }

    private boolean isPapiPresent() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

}
