package com.glance.codex.platform.paper.collectable.factory;

import com.glance.codex.api.collectable.Collectable;
import com.glance.codex.api.collectable.config.RepositoryConfig;
import com.glance.codex.platform.paper.collectable.BaseCollectableRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface BaseCollectableRepoFactory {

    @NotNull
    BaseCollectableRepository create(
        @NotNull RepositoryConfig cfg,
        @NotNull Map<String, Collectable> entries
    );

}
