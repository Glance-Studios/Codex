package com.glance.codex.platform.paper.api.collectable.type;

import com.glance.codex.platform.paper.api.collectable.Collectable;
import com.glance.codex.platform.paper.config.engine.codec.TypeCodec;
import org.jetbrains.annotations.NotNull;

public interface CollectableType {
    @NotNull String id();
    @NotNull Class<? extends Collectable> type();
    @NotNull
    TypeCodec<? extends Collectable> codec();
}
