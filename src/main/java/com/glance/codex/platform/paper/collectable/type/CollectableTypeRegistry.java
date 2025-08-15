package com.glance.codex.platform.paper.collectable.type;

import com.glance.codex.platform.paper.api.collectable.type.CollectableType;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Singleton
@AutoService(Manager.class)
public class CollectableTypeRegistry implements Manager {

    private final Map<String, CollectableType> types = new HashMap<>();

    public void register(@NotNull CollectableType type) {
        types.put(type.id().toLowerCase(Locale.ROOT), type);
    }

    public Optional<CollectableType> get(@NotNull String id) {
        return Optional.ofNullable(types.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<CollectableType> getOr(@NotNull String id, @NotNull String base) {
        var getFirst = types.get(id.toLowerCase(Locale.ROOT));
        if (getFirst == null) {
            return Optional.ofNullable(types.get(base.toLowerCase(Locale.ROOT)));
        }
        return Optional.of(getFirst);
    }

    public Collection<CollectableType> all() {
        return types.values();
    }

}
