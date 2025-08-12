package com.glance.codex.platform.paper.api.collectable.base.factory;

import com.glance.codex.platform.paper.api.collectable.base.BaseCollectableRepository;
import com.glance.codex.platform.paper.api.collectable.config.RepositoryConfig;
import org.jetbrains.annotations.NotNull;

public interface BaseCollectableRepoFactory {

    @NotNull
    BaseCollectableRepository create(
        @NotNull RepositoryConfig<?> cfg
    );

}
