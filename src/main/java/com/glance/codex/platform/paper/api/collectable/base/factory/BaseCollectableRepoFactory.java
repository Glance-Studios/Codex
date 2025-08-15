package com.glance.codex.platform.paper.api.collectable.base.factory;

import com.glance.codex.platform.paper.api.collectable.Collectable;
import com.glance.codex.platform.paper.api.collectable.base.BaseCollectableRepository;
import com.glance.codex.platform.paper.api.collectable.config.RepositoryConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface BaseCollectableRepoFactory {

    @NotNull
    BaseCollectableRepository create(
        @NotNull RepositoryConfig cfg,
        @NotNull Map<String, Collectable> entries
    );

}
