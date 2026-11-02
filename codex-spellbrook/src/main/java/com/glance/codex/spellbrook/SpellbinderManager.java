package com.glance.codex.spellbrook;

import com.glance.codex.api.collectable.CollectableManager;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import gg.spellbrook.magic.event.WandSpellCastEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Fills the Spellbinder in, one entry the first time a player casts each spell.
 *
 * <p>A {@link Manager} rather than a plain {@link Listener}, because a discovered listener is
 * registered unconditionally and registering one against an event class the server has not supplied
 * is what actually throws.
 */
@Singleton
@AutoService(Manager.class)
public class SpellbinderManager implements Manager, Listener {

    public static final String NAMESPACE = "spellbinder";

    private static final String PROBE = "gg.spellbrook.magic.event.WandSpellCastEvent";
    private static final String ONLY = "so the Spellbinder only fills in from 'collectables unlock'";

    private final Plugin plugin;
    private final CollectableManager collectables;

    @Inject
    public SpellbinderManager(@NotNull Plugin plugin, @NotNull CollectableManager collectables) {
        this.plugin = plugin;
        this.collectables = collectables;
    }

    @Override
    public void onEnable() {
        if (!eventPresent()) {
            // Named apart because a magic plugin too old to publish the event looks exactly like no
            // magic plugin at all, and sends whoever reads this after the wrong thing
            plugin.getLogger().info(loaded()
                    ? "The loaded SB-Magic does not publish WandSpellCastEvent, " + ONLY
                    : "SB-Magic is not loaded, " + ONLY);
            return;
        }
        listen();
        plugin.getLogger().info("Recording spell casts in the Spellbinder");
    }

    private boolean loaded() {
        return plugin.getServer().getPluginManager().getPlugin("SB-Magic") != null;
    }

    private boolean eventPresent() {
        try {
            Class.forName(PROBE, false, getClass().getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void listen() {
        plugin.getServer().getPluginManager().registerEvent(
                WandSpellCastEvent.class,
                this,
                EventPriority.MONITOR,
                (listener, event) -> record((WandSpellCastEvent) event),
                plugin,
                true
        );
    }

    // The event fires only once the cast has succeeded, so no further check is needed here
    private void record(@NotNull WandSpellCastEvent event) {
        String id = event.getSpell().getId();
        if (id == null || id.isBlank()) return;

        // fromString rather than the constructor: an id the server allows but a key does not would
        // otherwise throw inside the handler on every cast of that spell
        NamespacedKey key = NamespacedKey.fromString(NAMESPACE + ":" + id);
        if (key == null) {
            plugin.getLogger().warning("Spell id '" + id + "' cannot be a collectable key");
            return;
        }

        collectables.unlock(event.getPlayer(), key).exceptionally(error -> {
            plugin.getLogger().log(Level.WARNING, "Could not record a cast of " + id, error);
            return false;
        });
    }
}
