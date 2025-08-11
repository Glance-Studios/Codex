package com.glance.codex.platform.paper.notebooks.config;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigPath;
import com.glance.codex.platform.paper.config.model.BookConfig;
import com.google.auto.service.AutoService;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
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

    @ConfigPath("book")
    private Map<String, BookConfig> books = new LinkedHashMap<>();

}
