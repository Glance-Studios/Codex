package com.glance.codex.platform.paper.collectable.type;

import com.glance.codex.platform.paper.api.collectable.type.CollectableType;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Singleton
@AutoService(Manager.class)
public class CollectableTypeRegistry implements Manager {

    private final Map<String, CollectableType> types = new HashMap<>();

    public void register(@NotNull CollectableType type) {
        types.put(type.id().toLowerCase(Locale.ROOT), type);
    }

    public @Nullable CollectableType get(@NotNull String id) {
        return types.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<CollectableType> all() {
        return types.values();
    }

}
