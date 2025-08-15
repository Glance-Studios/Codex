package com.glance.codex.platform.paper.inject;

import com.glance.codex.platform.paper.api.collectable.CollectableManager;
import com.glance.codex.platform.paper.api.collectable.CollectableRepository;
import com.glance.codex.platform.paper.api.collectable.base.BaseCollectableRepository;
import com.glance.codex.platform.paper.api.collectable.base.factory.BaseCollectableRepoFactory;
import com.glance.codex.api.data.storage.CollectableStorage;
import com.glance.codex.platform.paper.api.text.PlaceholderService;
import com.glance.codex.platform.paper.collectable.manager.BaseCollectableManager;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.platform.paper.notebooks.book.BaseNotebookRegistry;
import com.glance.codex.platform.paper.persistence.config.CollectableStorageProvider;
import com.glance.codex.platform.paper.text.DefaultPlaceholderService;
import com.glance.codex.platform.paper.text.PapiPlaceholderService;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
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

        bind(CollectableStorage.class)
                .toProvider(CollectableStorageProvider.class)
                .in(Singleton.class);

        this.bind(CollectableManager.class).to(BaseCollectableManager.class).asEagerSingleton();
        this.bind(NotebookRegistry.class).to(BaseNotebookRegistry.class).asEagerSingleton();

        if (isPapiPresent()) {
            this.bind(PlaceholderService.class).to(PapiPlaceholderService.class).asEagerSingleton();
        } else {
            this.bind(PlaceholderService.class).to(DefaultPlaceholderService.class).asEagerSingleton();
        }

        this.install(new FactoryModuleBuilder()
                .implement(CollectableRepository.class, BaseCollectableRepository.class)
                .build(BaseCollectableRepoFactory.class));
    }

    private boolean isPapiPresent() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

}
