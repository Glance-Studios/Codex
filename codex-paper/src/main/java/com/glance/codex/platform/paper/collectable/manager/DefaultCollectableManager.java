package com.glance.codex.platform.paper.collectable.manager;

import com.glance.codex.api.collectable.*;
import com.glance.codex.api.collectable.base.PlayerCollectable;
import com.glance.codex.api.collectable.config.RepositoryConfig;
import com.glance.codex.api.data.storage.CollectableStorage;
import com.glance.codex.api.text.PlaceholderService;
import com.glance.codex.platform.paper.collectable.config.EntryParser;
import com.glance.codex.platform.paper.collectable.factory.CollectableRepoFactory;
import com.glance.codex.platform.paper.collectable.type.CollectableTypeRegistry;
import com.glance.codex.platform.paper.command.executor.CommandExecutorService;
import com.glance.codex.platform.paper.config.engine.ConfigController;
import com.glance.codex.platform.paper.text.PlaceholderUtils;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@Slf4j
@Singleton
@AutoService(Manager.class)
public class DefaultCollectableManager implements CollectableManager {

    private final Plugin plugin;
    private final Injector injector;
    private final Provider<CollectableStorage> storageProvider;
    private final CommandExecutorService commandExecutor;
    private final CollectableRepoFactory repositoryFactory;
    private final CollectableTypeRegistry typeRegistry;
    private final PlaceholderService placeholderService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final Map<String, CollectableRepository> repositories = new ConcurrentHashMap<>();

    @Inject
    public DefaultCollectableManager(
        @NotNull final Plugin plugin,
        @NotNull final Injector injector,
        @NotNull final Provider<CollectableStorage> storage,
        @NotNull final CollectableTypeRegistry typeRegistry,
        @NotNull final PlaceholderService placeholderService,
        @NotNull final CommandExecutorService commandExecutor,
        @NotNull final CollectableRepoFactory repositoryFactory
    ) {
        this.plugin = plugin;
        this.injector = injector;
        this.storageProvider = storage;
        this.typeRegistry = typeRegistry;
        this.commandExecutor = commandExecutor;
        this.repositoryFactory = repositoryFactory;
        this.placeholderService = placeholderService;
    }

    @Override
    public void onEnable() {
        // initialize storage instance
        this.storageProvider.get();
    }

    @Override
    public void loadFromConfig(@NotNull RepositoryConfig config) {
        if (!config.enabled()) return;

        Map<String, Collectable> entries;
        CollectableRepository repository;

        ConfigurationSection entriesSection = config.rawEntries();

        if (entriesSection == null) {
            entries = Map.of();
        } else {
            BiFunction<Collectable, ConfigurationSection, Boolean> binder = (inst, section) -> {
                ConfigController.populate(inst, section, null, true);
                return true;
            };

            entries = EntryParser.parseAndPopulate(
                    entriesSection,
                    EntryParser.getFactory(typeRegistry, "type"),
                    binder,
                    injector
            );
        }

        plugin.getLogger().info(
            "Registered Repository: '" + config.namespace() + "' with "
                    + entries.size() + " entries"
        );

        repository = this.repositoryFactory.create(config, entries);

        entries.forEach((entryId, c) ->
                c.setMeta(new CollectableMeta(config.namespace(), entryId, repository)));

        registerRepository(repository);
    }

    @Override
    public void registerRepository(CollectableRepository repo) {
        repositories.put(repo.namespace(), repo);
    }

    @Override
    public Collection<CollectableRepository> getRepositories() {
        return repositories.values();
    }

    @Override
    public @Nullable Collectable get(@NotNull NamespacedKey key) {
        CollectableRepository repo = repositories.get(key.namespace());
        return repo != null ? repo.get(key) : null;
    }

    @Override
    public @Nullable CollectableRepository getRepo(@NotNull String namespace) {
       return repositories.get(namespace);
    }

    @Override
    public CompletableFuture<Boolean> unlock(@NotNull Player player, NamespacedKey key) {
        var storage = storageProvider.get();
        Collectable collectable = get(key);
        if (collectable == null) return CompletableFuture.completedFuture(false);
        log.debug("Have Collectable here: {} is {}", collectable, collectable.getClass());

        final UUID uuid = player.getUniqueId();
        final String ns = key.namespace();
        final String id = key.getKey();
        final CollectableRepository repo = repositories.get(ns);

        var placeholderBuild = PlaceholderUtils.appendCollectableTags(
                key, collectable, null);
        placeholderBuild = PlaceholderUtils.appendPlayerTags(player, placeholderBuild);
        final Map<String, String> placeholders = placeholderBuild;

        return storage.isUnlocked(uuid, ns, id).thenCompose(unlocked -> {
            if (unlocked) {
                return performReplay(player, uuid, ns, id, collectable, placeholders);
            } else {
                // First unlock
                long now = System.currentTimeMillis();
                return storage.putUnlock(uuid, ns, id, now).thenApply(inserted -> {
                   if (!inserted) return false;
                   if (collectable instanceof Discoverable d) d.onDiscover(player);

                   if (collectable instanceof PlayerCollectable pc) {
                       if (pc.commandsOnDiscover() != null) {
                           commandExecutor.execute(pc.commandsOnDiscover(), player, placeholders);
                       }
                       sendGlobalMessage(player, pc.globalMessageOnDiscover(), placeholders);
                       sendPlayerMessage(player, pc.playerMessageOnDiscover(), placeholders);
                   }
                   return true;
                });
            }
        }).exceptionally(ex -> {
            plugin.getLogger().severe("[Collectables] Unlock failed for " + uuid +
                    " " + ns + ":" + id + " - " + ex);
           return false;
        });
    }

    private CompletableFuture<Boolean> performReplay(
        @NotNull Player player,
        @NotNull UUID uuid,
        @NotNull String ns,
        @NotNull String id,
        @NotNull Collectable collectable,
        @NotNull Map<String, String> placeholders
    ) {
        if (!collectable.allowReplay()) {
            return CompletableFuture.completedFuture(false);
        }

        final long now = System.currentTimeMillis();

        CompletableFuture<Void> storeStage;
        if (collectable.trackReplays()) {
            storeStage = storageProvider.get().recordReplay(uuid, ns, id, now);
        } else {
            storeStage = CompletableFuture.completedFuture(null);
        }

        return storeStage.thenApply(v -> {
           if (collectable instanceof Discoverable d) d.onReplay(player);
           if (collectable instanceof PlayerCollectable pc) {
               if (pc.commandsOnReplay() != null && !pc.commandsOnReplay().isEmpty()) {
                   this.commandExecutor.execute(pc.commandsOnReplay(), player, placeholders);
               }
               sendGlobalMessage(player, pc.globalMessageOnReplay(), placeholders);
               sendPlayerMessage(player, pc.playerMessageOnReplay(), placeholders);
           }
           return true;
        });
    }

    private void sendPlayerMessage(
        @NotNull Player player,
        @Nullable String raw,
        @NotNull Map<String, String> placeholders
    ) {
        if (raw == null || raw.isBlank()) return;
        if (!player.isOnline()) return;

        String msg = placeholderService.apply(raw, player, placeholders);
        player.sendMessage(mm.deserialize(msg));
    }

    private void sendGlobalMessage(
        @NotNull Player unlocker,
        @Nullable String raw,
        @NotNull Map<String, String> placeholders
    ) {
        if (raw == null || raw.isBlank()) return;
        String msg = placeholderService.apply(raw, unlocker, placeholders);

        Bukkit.getServer().broadcast(mm.deserialize(msg));
    }

    @Override
    public CompletableFuture<Boolean> isUnlocked(@NotNull Player player, NamespacedKey key) {
        return storageProvider.get().isUnlocked(player.getUniqueId(), key.getNamespace(), key.getKey());
    }

    @Override
    public CompletableFuture<Set<String>> unlockedIds(@NotNull Player player, @NotNull String namespace) {
        return storageProvider.get().loadUnlockedIds(player.getUniqueId(), namespace);
    }

    @Override
    public CompletableFuture<Boolean> relock(@NotNull Player player, @NotNull NamespacedKey key) {
        return storageProvider.get().deleteUnlock(player.getUniqueId(), key.getNamespace(), key.getKey());
    }

    @Override
    public CompletableFuture<Integer> clearRepo(@NotNull Player player, @NotNull String namespace) {
        return storageProvider.get().clearNamespace(player.getUniqueId(), namespace);
    }

    @Override
    public CompletableFuture<Void> clearAll(@NotNull Player player) {
        return storageProvider.get().clearAll(player.getUniqueId());
    }

}
