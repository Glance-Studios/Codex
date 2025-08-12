package com.glance.codex.platform.paper.collectable.manager;

import com.glance.codex.platform.paper.api.collectable.Collectable;
import com.glance.codex.platform.paper.api.collectable.CollectableManager;
import com.glance.codex.platform.paper.api.collectable.CollectableRepository;
import com.glance.codex.platform.paper.api.collectable.Discoverable;
import com.glance.codex.platform.paper.api.collectable.config.RepositoryConfig;
import com.glance.codex.platform.paper.api.collectable.base.factory.BaseCollectableRepoFactory;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@AutoService(Manager.class)
public class BaseCollectableManager implements CollectableManager {

    private final BaseCollectableRepoFactory repositoryFactory;

    private final Map<String, CollectableRepository> repositories = new ConcurrentHashMap<>();

    @Inject
    public BaseCollectableManager(
            @NotNull final BaseCollectableRepoFactory repositoryFactory
    ) {
        this.repositoryFactory = repositoryFactory;
    }

    @Override
    public CollectableRepository loadFromConfig(RepositoryConfig<? extends Collectable> config) {
        return this.repositoryFactory.create(config.namespace(), new LinkedHashMap<>(config.entries()));
    }

    @Override
    public void registerRepository(CollectableRepository repo) {
        repositories.put(repo.namespace(), repo);
    }

    @Override
    public @Nullable Collectable get(@NotNull NamespacedKey key) {
        CollectableRepository repo = repositories.get(key.namespace());
        return repo != null ? repo.get(key) : null;
    }

    @Override
    public boolean unlock(@NotNull Player player, NamespacedKey key) {
        // todo storage system

        Collectable collectable = get(key);
        if (collectable == null) return false;

        // todo unlock in storage
        if (collectable instanceof Discoverable d) d.onDiscover(player);
        return true;
    }

    @Override
    public boolean isUnlocked(@NotNull Player player, NamespacedKey key) {
        // todo check storage or local cache?
        return false;
    }

}
