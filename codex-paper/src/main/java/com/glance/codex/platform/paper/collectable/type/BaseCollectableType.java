package com.glance.codex.platform.paper.collectable.type;

import com.glance.codex.api.collectable.Collectable;
import com.glance.codex.api.collectable.base.BaseCollectable;
import com.glance.codex.api.collectable.type.CollectableType;
import com.glance.codex.platform.paper.config.engine.codec.TypeCodec;
import com.glance.codex.platform.paper.config.engine.codec.base.ConfigSerializableCodec;
import com.google.auto.service.AutoService;
import org.jetbrains.annotations.NotNull;

@AutoService(CollectableType.class)
public class BaseCollectableType implements CollectableType {

    @Override
    public @NotNull String id() {
        return "base";
    }

    @Override
    public @NotNull Class<? extends Collectable> type() {
        return BaseCollectable.class;
    }

    @Override
    public @NotNull TypeCodec<? extends Collectable> codec() {
        return new ConfigSerializableCodec<>(BaseCollectable.class);
    }

}
