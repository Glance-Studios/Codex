package com.glance.codex.platform.paper;

import com.glance.codex.platform.paper.config.engine.ConfigController;
import com.glance.codex.platform.paper.inject.CodexModule;
import com.glance.codex.platform.paper.inject.PaperComponentScanner;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class CodexPlugin extends JavaPlugin {

    @Getter
    private Injector injector;

    @Override
    public void onLoad() {
        this.injector = Guice.createInjector(new CodexModule(this));
        getLogger().setLevel(Level.FINE);

        ConfigController.init();
    }

    @Override
    public void onEnable() {
        PaperComponentScanner.scanAndInitialize(this, this.injector);
    }

    @Override
    public void onDisable() {
        PaperComponentScanner.scanAndCleanup(this, this.injector);
    }

    public static CodexPlugin getInstance() {
        return getPlugin(CodexPlugin.class);
    }

}
