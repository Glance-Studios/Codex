package com.glance.codex.api.collectable.type;

import com.glance.codex.api.collectable.Collectable;
import com.glance.codex.utils.data.TypeCodec;
import org.jetbrains.annotations.NotNull;

public interface CollectableType {
    @NotNull String id();
    @NotNull Class<? extends Collectable> type();
    @NotNull
    TypeCodec<? extends Collectable> codec();
}
