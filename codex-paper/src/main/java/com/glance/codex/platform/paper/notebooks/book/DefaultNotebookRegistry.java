package com.glance.codex.platform.paper.notebooks.book;

import com.glance.codex.platform.paper.config.engine.event.ConfigClassReloadEvent;
import com.glance.codex.platform.paper.config.model.BookConfig;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.platform.paper.notebooks.config.NoteBookConfig;
import com.glance.codex.platform.paper.notebooks.config.NoteBookConfigLoader;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
@AutoService({Manager.class, Listener.class})
public class DefaultNotebookRegistry implements Listener, NotebookRegistry {

    private final Plugin plugin;
    private final NoteBookRenderService renderService;

    private final Map<NamespacedKey, BookConfig> byKey = new ConcurrentHashMap<>();

    @Inject
    public DefaultNotebookRegistry(
            @NotNull final Plugin plugin,
            @NotNull final NoteBookRenderService renderService
    ) {
        this.plugin = plugin;
        this.renderService = renderService;
    }

    @Override
    public Map<NamespacedKey, BookConfig> all() {
        return Map.copyOf(byKey);
    }

    @Override
    public void register(@NotNull String namespace, @NotNull String id, @NotNull BookConfig cfg) {
        if (!cfg.enabled()) return;
        NamespacedKey key = new NamespacedKey(namespace, id);
        byKey.put(key, cfg);
    }

    @Override
    public void unregisterNamespace(@NotNull String namespace) {
        byKey.keySet().removeIf(k -> k.namespace().equals(namespace));
    }

    @Override
    public Optional<BookConfig> get(@NotNull NamespacedKey id) {
        return Optional.ofNullable(byKey.get(id));
    }

    @Override
    public boolean exists(@NotNull NamespacedKey id) {
        return byKey.containsKey(id);
    }

    @Override
    public boolean give(@NotNull NamespacedKey id, @NotNull Player player) {
        BookConfig cfg = this.get(id).orElse(null);
        if (cfg == null || !cfg.enabled()) return false;

        // todo give item?
        return false;
    }

    @Override
    public boolean open(@NotNull NamespacedKey id, @NotNull Player player) {
        BookConfig cfg = this.get(id).orElse(null);
        if (cfg == null || !cfg.enabled()) return false;

        showToPlayer(cfg, player, null);
        return true;
    }

    private void showToPlayer(
            @NotNull BookConfig cfg,
            @NotNull Player player,
            @Nullable Map<String, String> placeholders
    ) {
        ItemStack book = renderService.buildWrittenBook(cfg, player, placeholders);
        plugin.getServer().getScheduler().runTask(plugin, () -> player.openBook(book));
    }

    @Override
    public void onDisable() {
        clear();
    }

    public void clear() {
        this.byKey.clear();
    }

    @EventHandler
    public void onNotebooksReload(ConfigClassReloadEvent event) {
        if (!NoteBookConfig.class.isAssignableFrom(event.configClass())) return;
        clear();

        List<NoteBookConfig> noteConfigs = event.instances()
                .stream().map(h -> (NoteBookConfig) h).toList();

        NoteBookConfigLoader.handleNoteBooks(this, noteConfigs);
    }

}
