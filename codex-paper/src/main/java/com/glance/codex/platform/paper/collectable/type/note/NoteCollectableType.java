package com.glance.codex.platform.paper.collectable.type.note;

import com.glance.codex.api.collectable.Collectable;
import com.glance.codex.api.collectable.type.CollectableType;
import com.glance.codex.platform.paper.config.engine.codec.base.ConfigSerializableCodec;
import com.glance.codex.utils.data.TypeCodec;
import com.google.auto.service.AutoService;
import org.jetbrains.annotations.NotNull;

@AutoService(CollectableType.class)
public class NoteCollectableType implements CollectableType {

    @Override
    public @NotNull String id() {
        return "note";
    }

    @Override
    public @NotNull Class<? extends Collectable> type() {
        return NoteCollectable.class;
    }

    // todo ensure this works
    @Override
    public @NotNull TypeCodec<? extends Collectable> codec() {
        return new ConfigSerializableCodec<>(NoteCollectable.class);
    }

}
