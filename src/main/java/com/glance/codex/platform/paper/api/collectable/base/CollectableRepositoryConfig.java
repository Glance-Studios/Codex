package com.glance.codex.platform.paper.api.collectable.base;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigPath;
import com.google.auto.service.AutoService;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Accessors(fluent = true)
@Config(fileName = "collectables/*")
@AutoService(Config.Handler.class)
public class CollectableRepositoryConfig implements Config.Handler {

    @ConfigPath("namespace")
    private String namespace;

    @ConfigPath("display_name")
    private String displayName;

    @ConfigPath("display_name_raw")
    private String displayNameRaw;

    @ConfigPath("show_when_locked")
    private boolean showWhenLocked = true;

    @ConfigPath("entries")
    private Map<String, BaseCollectable> entries = new LinkedHashMap<>();

}
