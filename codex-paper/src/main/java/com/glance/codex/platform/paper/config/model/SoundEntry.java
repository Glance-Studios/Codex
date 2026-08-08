package com.glance.codex.platform.paper.config.model;

import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A configurable sound cue played to a single player
 * <p>
 * The {@code sound} value is a raw sound key rather than a Bukkit enum, so both vanilla
 * sounds and custom sounds shipped in a resource pack (Nexo, ItemsAdder, etc) can be used
 * <p>
 * Example:
 * <pre><code>
 * openSound:
 *   enabled: true
 *   sound: "minecraft:item.book.page_turn"
 *   volume: 1.0
 *   pitch: 1.0
 *   source: MASTER
 * </code></pre>
 *
 * @author Cammy
 */
@Slf4j
@Data
@Accessors(fluent = true)
public class SoundEntry implements ConfigSerializable {

    /** Vanilla page turn, used as the out of the box book open cue */
    public static final String BOOK_PAGE_TURN = "minecraft:item.book.page_turn";

    /*
     * Boxed on purpose. The config engine reads an absent key through
     * ConfigurationSection.getBoolean/getDouble, which answer false/0 rather than null,
     * so a primitive field cannot tell "not set" from "set to the zero value" and would
     * silently decode an omitted 'enabled' as false or an omitted 'volume' as silence.
     * Null means not set, and the accessors below supply the real default.
     */

    /**
     * Whether this cue is played
     * <p>
     * Set false to silence a sound that would otherwise be inherited from a default
     */
    @ConfigField(order = 1)
    private Boolean enabled;

    /**
     * Sound key to play, e.g. {@code minecraft:item.book.page_turn}
     * <p>
     * A key with no namespace is treated as {@code minecraft:}. Blank means no sound
     */
    @ConfigField(order = 2)
    private String sound = "";

    @ConfigField(order = 3)
    private Float volume;

    @ConfigField(order = 4)
    private Float pitch;

    /**
     * Mixer category the sound is played on, matching the client's volume sliders
     * <p>
     * One of MASTER, MUSIC, RECORD, WEATHER, BLOCK, HOSTILE, NEUTRAL, PLAYER, AMBIENT, VOICE
     */
    @ConfigField(order = 5)
    private Sound.Source source = Sound.Source.MASTER;

    /**
     * Ticks to wait before this layer plays, 20 per second
     * <p>
     * Layers of a cue all fire together by default. A small offset is useful when one
     * sound should sit under another, e.g. a page turn with a chime a few ticks later
     */
    @ConfigField(order = 6)
    private Long delayTicks;

    /* Defaults for anything the config left out */

    public boolean enabled() {
        return enabled == null || enabled;
    }

    public float volume() {
        return volume != null ? volume : 1.0f;
    }

    public float pitch() {
        return pitch != null ? pitch : 1.0f;
    }

    public long delayTicks() {
        return delayTicks != null ? delayTicks : 0L;
    }

    /**
     * Build the Adventure sound this entry describes
     *
     * @return the sound, or null if disabled, blank, or the key is malformed
     */
    public @Nullable Sound toAdventure() {
        if (!enabled()) return null;
        if (sound == null || sound.isBlank()) return null;

        try {
            return Sound.sound(
                    Key.key(sound),
                    source != null ? source : Sound.Source.MASTER,
                    volume(),
                    pitch()
            );
        } catch (Exception e) {
            log.warn("Invalid sound key '{}', no sound will play. Expected e.g. '{}'",
                    sound, BOOK_PAGE_TURN);
            return null;
        }
    }

    /** Create an enabled entry for the given sound key at default volume and pitch */
    public static @NotNull SoundEntry of(@NotNull String sound) {
        SoundEntry entry = new SoundEntry();
        entry.sound = sound;
        return entry;
    }

    /**
     * Flatten the two config forms of a cue into one list of layers
     * <p>
     * A config may declare either a list of layers or a single sound. The list wins when
     * both are present
     *
     * @param layers the plural form, may be null or empty
     * @param single the singular form, may be null
     * @return the layers to play, never null, empty when nothing is configured
     */
    public static @NotNull List<SoundEntry> resolveLayers(
            @Nullable List<SoundEntry> layers,
            @Nullable SoundEntry single
    ) {
        if (layers != null && !layers.isEmpty()) return layers;
        if (single != null) return List.of(single);
        return List.of();
    }

}
