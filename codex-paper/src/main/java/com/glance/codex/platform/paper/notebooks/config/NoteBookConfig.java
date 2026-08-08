package com.glance.codex.platform.paper.notebooks.config;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigPath;
import com.glance.codex.platform.paper.config.model.BookConfig;
import com.glance.codex.platform.paper.config.model.SoundEntry;
import com.google.auto.service.AutoService;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(fluent = true)
@Config(path = "collectables/notes/**", writeDefaults = false)
@AutoService(Config.Handler.class)
public class NoteBookConfig implements Config.Contract {

    @ConfigPath(value = "namespace", comments = "The collectables namespace for these book")
    private String namespace;

    @ConfigPath(value = "id", comments = "Override default id of 'filename' when using singular book")
    private String id;

    @ConfigPath(value = "book", comments = "Single-book form; should not use if 'book' map is present")
    private BookConfig book;

    @ConfigPath("books")
    private Map<String, BookConfig> books = new LinkedHashMap<>();

    @ConfigPath(value = "default_open_sound", comments = {
        "Sound played when any book in this file is opened.",
        "A book's own 'openSound' or 'openSounds' overrides this;",
        "set 'enabled: false' to silence all of them."
    })
    private SoundEntry defaultOpenSound = SoundEntry.of(SoundEntry.BOOK_PAGE_TURN);

    @ConfigPath(value = "default_open_sounds", comments = {
        "Layered form of the above: several sounds making up one open cue.",
        "Takes precedence over 'default_open_sound'. Layers play together unless",
        "a layer sets 'delayTicks'."
    })
    private List<SoundEntry> defaultOpenSounds;

    /**
     * The file level open cue as a flat list of layers, whichever form it was configured in
     *
     * @return the layers to fall back to, empty if this file configures none
     */
    public @NotNull List<SoundEntry> defaultOpenSoundLayers() {
        return SoundEntry.resolveLayers(defaultOpenSounds, defaultOpenSound);
    }

}
