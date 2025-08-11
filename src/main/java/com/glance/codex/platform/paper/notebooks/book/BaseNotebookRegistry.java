package com.glance.codex.platform.paper.notebooks.book;

import com.glance.codex.platform.paper.config.model.BookConfig;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@AutoService(Manager.class)
public class BaseNotebookRegistry implements Manager, NotebookRegistry {

    private final Plugin plugin;
    private final Map<NamespacedKey, BookConfig> byKey = new ConcurrentHashMap<>();

    @Inject
    public BaseNotebookRegistry(@NotNull final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Map<NamespacedKey, BookConfig> all() {
        return Map.copyOf(byKey);
    }

    @Override
    public void register(String namespace, String id, BookConfig cfg) {
        if (!cfg.enabled()) return;
        NamespacedKey key = new NamespacedKey(namespace, id);
        byKey.put(key, cfg);
    }

    @Override
    public void unregisterNamespace(String namespace) {
        byKey.keySet().removeIf(k -> k.namespace().equals(namespace));
    }

    @Override
    public Optional<BookConfig> get(NamespacedKey id) {
        return Optional.empty();
    }

    @Override
    public boolean exists(NamespacedKey id) {
        return false;
    }

    @Override
    public boolean give(NamespacedKey id, Player player) {
        return false;
    }

    @Override
    public boolean open(NamespacedKey id, Player player) {
        return false;
    }

    @Override
    public void reload() {}

}
