package com.glance.codex.platform.paper.inject;

import com.glance.codex.bootstrap.GuiceServiceLoader;
import com.glance.codex.platform.paper.api.collectable.CollectableManager;
import com.glance.codex.platform.paper.api.collectable.CollectableRepository;
import com.glance.codex.platform.paper.api.collectable.config.RepositoryConfig;
import com.glance.codex.platform.paper.collectable.config.CollectableRepositoryConfig;
import com.glance.codex.platform.paper.api.collectable.type.CollectableType;
import com.glance.codex.platform.paper.command.engine.CommandHandler;
import com.glance.codex.platform.paper.command.engine.CommandManager;
import com.glance.codex.platform.paper.command.engine.argument.TypedArgParser;
import com.glance.codex.platform.paper.config.engine.ConfigController;
import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.notebooks.config.NoteBookConfig;
import com.glance.codex.platform.paper.config.model.BookConfig;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.platform.paper.collectable.type.CollectableTypeRegistry;
import com.glance.codex.platform.paper.notebooks.config.NoteBookConfigLoader;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.experimental.UtilityClass;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> Utility component scanner to discover SPI registered classes </p>
 * <p> Constructs instances via {@link Guice} </p>
 * By default, supports common/known plugin lifecycle types:
 * <li>{@link Listener}</li>
 * <li>{@link Manager}</li>
 * <li>{@link Config}</li>
 * <li>{@link CommandHandler}</li>
 * <li>Anything to come</li>
 * </p>
 * @author Cammy
 */
@UtilityClass
public class PaperComponentScanner {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PaperComponentScanner.class);

    @SuppressWarnings("unchecked")
    public void scanAndInitialize(@NotNull final Plugin plugin, @NotNull Injector injector) {
        Logger logger = plugin.getLogger();
        logger.info("[AutoScan] Starting auto component scan for " + plugin.getName());

        ClassLoader classLoader = plugin.getClass().getClassLoader();

        // Config Loading
        for (Class<? extends Config.Handler> clazz : GuiceServiceLoader.load(Config.Handler.class, classLoader)) {
            try {
                if (Config.Contract.class.isAssignableFrom(clazz)) continue;

                Config.Handler config = injector.getInstance(clazz);

                @SuppressWarnings("unchecked")
                Class<Config.Handler> typedClass = (Class<Config.Handler>) clazz;
                ConfigController.loadConfig(plugin, typedClass, config);
                logger.fine("[AutoScan] Loaded Config Bean: " + clazz.getName());
            } catch (Exception e) {
                logError(logger, "load config", clazz, e);
            }
        }

        // Manager Enable
        for (Class<? extends Manager> clazz : GuiceServiceLoader.load(
                Manager.class,
                classLoader)
        ) {
            try {
                Manager manager = injector.getInstance(clazz);
                manager.onEnable();
                logger.fine("[AutoScan] Enabled Manager: " + clazz.getName());
            } catch (Exception e) {
                logError(logger, "enable Manager", clazz, e);
            }
        }

        // Collectable Type Registration
        CollectableTypeRegistry collectableTypeRegistry = injector.getInstance(CollectableTypeRegistry.class);
        if (collectableTypeRegistry != null) {
            for (Class<? extends CollectableType> clazz : GuiceServiceLoader.load(CollectableType.class, classLoader)) {
                try {
                    CollectableType type = injector.getInstance(clazz);
                    collectableTypeRegistry.register(type);
                    logger.fine("[AutoScan] CollectableType Registered: " + clazz.getName());
                } catch (Exception e) {
                    logError(logger, "registering CollectableType", clazz, e);
                }
            }
        }

        // Collectable Config Loading - after manager setup
        CollectableManager collectableManager = injector.getInstance(CollectableManager.class);
        if (collectableManager != null) {
            for (Class<? extends Config.Handler> clazz : GuiceServiceLoader.load(Config.Handler.class, classLoader)) {
                try {
                    if (!RepositoryConfig.class.isAssignableFrom(clazz)) continue;

                    Class<CollectableRepositoryConfig> typedClass = (Class<CollectableRepositoryConfig>) clazz;

                    List<CollectableRepositoryConfig> repoConfigs = ConfigController
                            .loadByPattern(typedClass, plugin.getDataFolder(), injector);

                    for (CollectableRepositoryConfig cfg : repoConfigs) {
                        if (!cfg.enabled()) continue;
                        CollectableRepository repo = collectableManager.loadFromConfig(cfg);
                        collectableManager.registerRepository(repo);
                    }
                } catch (Exception e) {
                    logError(logger, "load collectable config", clazz, e);
                }
            }
        }

        // Scan NoteBook configs
        for (Class<? extends Config.Handler> clazz : GuiceServiceLoader.load(Config.Handler.class, classLoader)) {
            try {
                if (!NoteBookConfig.class.isAssignableFrom(clazz)) continue;

                Class<NoteBookConfig> typedClass = (Class<NoteBookConfig>) clazz;
                List<NoteBookConfig> noteConfigs = ConfigController
                        .loadByPattern(typedClass, plugin.getDataFolder(), injector);

                NoteBookConfigLoader.handleNoteBooks(injector, noteConfigs);
            } catch (Exception e) {
                logError(logger, "loading notebook configs", clazz, e);
            }
        }

        // Bukkit Event Listeners
        for (Class<? extends Listener> clazz : GuiceServiceLoader.load(
                Listener.class,
                classLoader)
        ) {
            try {
                Listener listener = injector.getInstance(clazz);
                plugin.getServer().getPluginManager().registerEvents(listener, plugin);
                logger.fine("[AutoScan] Registered Listener: " + clazz.getName());
            } catch (Exception e) {
                logError(logger, "register Listener", clazz, e);
            }
        }

        // Cloud commands
        CommandManager commandManager;
        try {
            commandManager = injector.getInstance(CommandManager.class);
            handleCommandComponents(commandManager, classLoader, injector, logger);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[AutoScan] Failed to initialize a Command Lifecycle element!", e);
        }

        logger.info("[AutoScan] Completed component scan for " + plugin.getName());
    }

    @SuppressWarnings("all")
    private static void handleCommandComponents(
            @NotNull CommandManager commandManager,
            @NotNull ClassLoader classLoader,
            @NotNull Injector injector,
            Logger log
    ) {
        // Annotation Parsers
        for (Class<? extends TypedArgParser> raw : GuiceServiceLoader.load(
                TypedArgParser.class,
                classLoader)
        ) {
            try {
                Class<? extends TypedArgParser<?>> clazz = (Class<? extends TypedArgParser<?>>) raw;

                TypedArgParser<?> instance = injector.getInstance(clazz);

                commandManager.parserRegistry().registerParserSupplier(
                        instance.typeToken(),
                        params -> injector.getInstance(clazz)
                );

                log.fine("[GuiceScan] Registered Argument Parser '"+ clazz.getName() + "' for: " + instance.type().getSimpleName());
            } catch (Exception e) {
                logError(log, "register parser", raw, e);
            }
        }

        // Command Handlers
        for (Class<? extends CommandHandler> clazz : GuiceServiceLoader.load(
                CommandHandler.class,
                classLoader)
        ) {
            CommandHandler command;
            try {
                command = injector.getInstance(clazz);
                commandManager.registerAnnotated(command);
                log.fine("[GuiceScan] Registered Command Handler: " + clazz.getName());
            } catch (Exception e) {
                logError(log, "register CommandHandler", clazz, e);
            }
        }
    }

    public void scanAndCleanup(@NotNull final Plugin plugin, @NotNull Injector injector) {
        Logger logger = plugin.getLogger();
        logger.info("Starting component cleanup for " + plugin.getName());

        ClassLoader classLoader = plugin.getClass().getClassLoader();

        // Manager Disable
        for (Class<? extends Manager> clazz : GuiceServiceLoader.load(
                Manager.class,
                classLoader)
        ) {
            try {
                Manager manager = injector.getInstance(clazz);
                manager.onDisable();
                logger.fine("[AutoScan] Disabling Manager: " + clazz.getName());
            } catch (Exception e) {
                logError(logger, "disabling Manager", clazz, e);
            }
        }

        logger.info("[AutoScan] Completed component cleanup for " + plugin.getName());
    }

    private static void logError(Logger log, String context, Class<?> clazz, Exception e) {
        log.log(Level.SEVERE, "[AutoScan] Failed to " + context + ": " + clazz.getName(), e);
    }

}
