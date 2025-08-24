package com.glance.codex.platform.paper.config.engine.reload;

import org.bukkit.configuration.ConfigurationSection;

import java.io.File;

public record ReloadResult(
    File file,
    ConfigurationSection section,
    boolean sectionChanged,
    boolean fieldsChanged
) {}
