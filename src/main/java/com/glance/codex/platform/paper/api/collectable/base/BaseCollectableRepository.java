package com.glance.codex.platform.paper.api.collectable.base;

import com.glance.codex.platform.paper.api.collectable.CollectableRepository;
import com.glance.codex.platform.paper.api.text.PlaceholderService;
import com.glance.codex.platform.paper.command.executor.CommandExecutorService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Map;

// todo factory
public class BaseCollectableRepository implements CollectableRepository<BaseCollectable> {

    private final String namespace;
    private final Map<String, BaseCollectable> entries;
    private final CommandExecutorService commandExecutor;
    private final PlaceholderService resolver;

    @Inject
    public BaseCollectableRepository(
        @Assisted String namespace,
        @Assisted Map<String, BaseCollectable> entries,
        @NotNull final CommandExecutorService commandExecutor,
        @NotNull final PlaceholderService resolver
    ) {
        this.namespace = namespace;
        this.entries = entries;
        this.commandExecutor = commandExecutor;
        this.resolver = resolver;
    }

    @Override
    public @NotNull String namespace() {
        return namespace;
    }

    @Override
    public @NotNull Map<String, BaseCollectable> entries() {
        return (entries != null) ? this.entries : Map.of();
    }

    @Override
    public @Nullable BaseCollectable get(@NotNull NamespacedKey key) {
        return null;
    }

    // todo unlock/replay here? To trigger the executor?


}
