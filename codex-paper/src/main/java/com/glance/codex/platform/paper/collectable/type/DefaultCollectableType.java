package com.glance.codex.platform.paper.collectable.type;

import com.glance.codex.api.collectable.Collectable;
import com.glance.codex.api.collectable.type.CollectableType;
import com.glance.codex.platform.paper.collectable.DefaultCollectable;
import com.glance.codex.platform.paper.config.engine.codec.base.ConfigSerializableCodec;
import com.glance.codex.utils.data.TypeCodec;
import com.google.auto.service.AutoService;
import org.jetbrains.annotations.NotNull;

@AutoService(CollectableType.class)
public class DefaultCollectableType implements CollectableType {

    @Override
    public @NotNull String id() {
        return "base";
    }

    @Override
    public @NotNull Class<? extends Collectable> type() {
        return DefaultCollectable.class;
    }

    @Override
    public @NotNull TypeCodec<? extends Collectable> codec() {
        return new ConfigSerializableCodec<>(DefaultCollectable.class);
    }

}
