package com.glance.codex.platform.paper.api.collectable.config;

import com.glance.codex.platform.paper.api.collectable.Collectable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface RepositoryConfig<T extends Collectable> {
    boolean enabled();
    String namespace();
    @NotNull
    Map<String, T> entries();
}
