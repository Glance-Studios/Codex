package com.glance.codex.platform.paper.collectable.config;

import com.glance.codex.platform.paper.api.collectable.base.BaseCollectable;
import com.glance.codex.platform.paper.api.collectable.config.RepositoryConfig;
import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigPath;
import com.google.auto.service.AutoService;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Accessors(fluent = true)
@Config(path = "collectables/*", writeDefaults = false)
@AutoService(Config.Handler.class)
public class CollectableRepositoryConfig implements Config.Contract, RepositoryConfig<BaseCollectable> {

    @ConfigPath("enabled")
    private boolean enabled;

    @ConfigPath("namespace")
    private String namespace = "";

    @ConfigPath("display_name")
    private String displayName = "";

    @ConfigPath("display_name_raw")
    private String displayNameRaw = "";

    @ConfigPath("show_when_locked")
    private boolean showWhenLocked = true;

    @ConfigPath("entries")
    private Map<String, BaseCollectable> entries = new LinkedHashMap<>();

}
