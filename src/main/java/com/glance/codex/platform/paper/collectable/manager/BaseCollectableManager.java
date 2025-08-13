package com.glance.codex.platform.paper.collectable.manager;

import com.glance.codex.platform.paper.api.collectable.Collectable;
import com.glance.codex.platform.paper.api.collectable.CollectableManager;
import com.glance.codex.platform.paper.api.collectable.CollectableRepository;
import com.glance.codex.platform.paper.api.collectable.Discoverable;
import com.glance.codex.platform.paper.api.collectable.base.BaseCollectable;
import com.glance.codex.platform.paper.api.collectable.config.RepositoryConfig;
import com.glance.codex.platform.paper.api.collectable.base.factory.BaseCollectableRepoFactory;
import com.glance.codex.platform.paper.api.data.storage.CollectableStorage;
import com.glance.codex.platform.paper.api.text.PlaceholderService;
import com.glance.codex.platform.paper.command.executor.CommandExecutorService;
import com.glance.codex.platform.paper.text.PlaceholderUtils;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
@AutoService(Manager.class)
public class BaseCollectableManager implements CollectableManager {

    private final Plugin plugin;
    private final CollectableStorage storage;
    private final CommandExecutorService commandExecutor;
    private final BaseCollectableRepoFactory repositoryFactory;

    private final Map<String, CollectableRepository> repositories = new ConcurrentHashMap<>();

    @Inject
    public BaseCollectableManager(
        @NotNull final Plugin plugin,
        @NotNull final CollectableStorage storage,
        @NotNull final CommandExecutorService commandExecutor,
        @NotNull final BaseCollectableRepoFactory repositoryFactory
    ) {
        this.plugin = plugin;
        this.storage = storage;
        this.commandExecutor = commandExecutor;
        this.repositoryFactory = repositoryFactory;
    }

    @Override
    public CollectableRepository loadFromConfig(RepositoryConfig<? extends Collectable> config) {
        return this.repositoryFactory.create(config);
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
        Collectable collectable = get(key);
        if (collectable == null) return CompletableFuture.completedFuture(false);

        final UUID uuid = player.getUniqueId();
        final String ns = key.namespace();
        final String id = key.getKey();
        final CollectableRepository repo = repositories.get(ns);

        return storage.isUnlocked(uuid, ns, id).thenCompose(unlocked -> {
            if (unlocked) {
                if (!collectable.allowReplay()) return CompletableFuture.completedFuture(false);
                long now = System.currentTimeMillis();
                return storage.recordReplay(uuid, ns, id, now).thenApply(v -> {
                    if (collectable instanceof Discoverable d) d.onReplay(player);
                    if (collectable instanceof BaseCollectable bc && bc.getCommandsOnReplay() != null) {
                        Map<String, String> placeholders = PlaceholderUtils.appendCollectableTags(
                           player, key, collectable, repo, null);
                        placeholders = PlaceholderUtils.appendPlayerTags(player, placeholders);

                        this.commandExecutor.execute(bc.getCommandsOnReplay(), player, placeholders);
                    }
                    return true;
                });
            } else {
                long now = System.currentTimeMillis();
                return storage.putUnlock(uuid, ns, id, now).thenApply(inserted -> {
                   if (!inserted) return false;
                   if (collectable instanceof Discoverable d) d.onDiscover(player);
                   if (collectable instanceof BaseCollectable bc && bc.getCommandsOnDiscover() != null) {
                       Map<String, String> placeholders = PlaceholderUtils.appendCollectableTags(
                               player, key, collectable, repo, null);
                       placeholders = PlaceholderUtils.appendPlayerTags(player, placeholders);
                       commandExecutor.execute(bc.getCommandsOnDiscover(), player, placeholders);
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

    @Override
    public CompletableFuture<Boolean> isUnlocked(@NotNull Player player, NamespacedKey key) {
        return storage.isUnlocked(player.getUniqueId(), key.getNamespace(), key.getKey());
    }

    @Override
    public CompletableFuture<Set<String>> unlockedIds(@NotNull Player player, @NotNull String namespace) {
        return storage.loadUnlockedIds(player.getUniqueId(), namespace);
    }

}
