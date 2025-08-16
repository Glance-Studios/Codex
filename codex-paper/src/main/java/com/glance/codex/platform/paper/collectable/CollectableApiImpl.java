package com.glance.codex.platform.paper.collectable;

import com.glance.codex.api.collectable.CollectableAPI;
import com.glance.codex.api.collectable.CollectableManager;
import com.glance.codex.api.collectable.type.CollectableType;
import com.glance.codex.platform.paper.collectable.type.CollectableTypeRegistry;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Singleton
public class CollectableApiImpl implements CollectableAPI {

    private final CollectableManager collectableManager;
    private final CollectableTypeRegistry typeRegistry;

    @Inject
    public CollectableApiImpl(
        @NotNull final CollectableManager collectableManager,
        @NotNull final CollectableTypeRegistry typeRegistry
    ) {
        this.collectableManager = collectableManager;
        this.typeRegistry = typeRegistry;
    }

    @Override
    public @NotNull CollectableManager collectables() {
        return this.collectableManager;
    }

    @Override
    public void registerCollectableType(@NotNull CollectableType type) {
        this.typeRegistry.register(type);
    }

    @Override
    public @Nullable Optional<CollectableType> getCollectableType(@NotNull String typeId) {
        return this.typeRegistry.get(typeId);
    }

    @Override
    public String apiVersion() {
        return "1.0.0";
    }
}
