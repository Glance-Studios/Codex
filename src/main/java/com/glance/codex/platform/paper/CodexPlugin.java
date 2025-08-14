package com.glance.codex.platform.paper;

import com.glance.codex.platform.paper.api.text.PlaceholderService;
import com.glance.codex.platform.paper.config.engine.ConfigController;
import com.glance.codex.platform.paper.config.engine.codec.ConfigInterpolator;
import com.glance.codex.platform.paper.inject.CodexModule;
import com.glance.codex.platform.paper.inject.PaperComponentScanner;
import com.glance.codex.platform.paper.text.PluginPlaceholderSource;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class CodexPlugin extends JavaPlugin {

    @Getter
    private Injector injector;

    private PlaceholderService placeholderService;

    @Override
    public void onLoad() {
        this.injector = Guice.createInjector(new CodexModule(this));
        getLogger().setLevel(Level.FINE);

        ConfigInterpolator.registerSource(new PluginPlaceholderSource(this));
        ConfigController.init();
    }

    @Override
    public void onEnable() {
        PaperComponentScanner.scanAndInitialize(this, this.injector);

        this.placeholderService = injector.getInstance(PlaceholderService.class);
    }

    @Override
    public void onDisable() {
        PaperComponentScanner.scanAndCleanup(this, this.injector);
    }

    public @NotNull PlaceholderService placeholderService() {
        if (this.placeholderService == null) {
            throw new IllegalStateException("PlaceholderService has not been initialized");
        }
        return this.placeholderService;
    }

    public static CodexPlugin getInstance() {
        return getPlugin(CodexPlugin.class);
    }

}
