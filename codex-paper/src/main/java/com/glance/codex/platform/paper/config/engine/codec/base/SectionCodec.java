package com.glance.codex.platform.paper.config.engine.codec.base;

import com.glance.codex.platform.paper.config.engine.codec.TypeCodec;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public final class SectionCodec implements TypeCodec<ConfigurationSection> {

    @Override
    public @Nullable ConfigurationSection decode(ConfigurationSection section, String path, Type type, @Nullable ConfigurationSection defaultValue) {
        return section.getConfigurationSection(path);
    }

    @Override
    public ConfigurationSection decodeFromRaw(@Nullable Object raw, @NotNull Type type, @Nullable ConfigurationSection defaultValue) {
        return (ConfigurationSection) raw;
    }

    @Override
    public @Nullable Object encode(ConfigurationSection value) {
        return value;
    }

}
