package com.glance.codex.api.collectable;

import com.glance.codex.api.collectable.type.CollectableType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface CollectableAPI {

    @NotNull
    CollectableManager collectables();

    void registerCollectableType(@NotNull CollectableType type);

    Optional<CollectableType> getCollectableType(@NotNull String typeId);

    String apiVersion();

}
