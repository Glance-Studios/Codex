package com.glance.codex.platform.paper.notebooks.book;

import com.glance.codex.platform.paper.config.model.SoundEntry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Plays a layered sound cue to a single player
 * <p>
 * Shared by the book open path and the authoring commands, so a previewed cue is exactly
 * the cue that will play in game
 *
 * @author Cammy
 */
@Singleton
public class SoundCuePlayer {

    private final Plugin plugin;

    @Inject
    public SoundCuePlayer(@NotNull final Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Play every layer of a cue to the given player
     * <p>
     * Layers with no delay fire immediately; the rest are scheduled so a cue can be
     * staggered. A delayed layer is dropped if the player leaves first
     *
     * @param layers the cue, may be empty
     * @param player who hears it
     */
    public void play(@NotNull List<SoundEntry> layers, @NotNull Player player) {
        for (SoundEntry layer : layers) {
            Sound sound = layer.toAdventure();
            if (sound == null) continue;

            long delay = layer.delayTicks();
            if (delay <= 0L) {
                player.playSound(sound, Sound.Emitter.self());
                continue;
            }

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.playSound(sound, Sound.Emitter.self());
                }
            }, delay);
        }
    }

}
