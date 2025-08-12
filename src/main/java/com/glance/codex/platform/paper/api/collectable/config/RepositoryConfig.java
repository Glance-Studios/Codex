package com.glance.codex.platform.paper.api.collectable.config;

import com.glance.codex.platform.paper.api.collectable.Collectable;
import com.glance.codex.platform.paper.config.model.ItemEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface RepositoryConfig<T extends Collectable> {
    boolean enabled();
    @NotNull
    String namespace();

    @NotNull
    ItemEntry icon();

    @NotNull
    ItemEntry selectedIcon();

    @NotNull
    Map<String, T> entries();
}
