package com.glance.codex.platform.paper.config.engine.codec.base;

import com.glance.codex.platform.paper.config.engine.codec.TypeCodec;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Codec for native Bukkit {@link ConfigurationSerializable} types
 *
 * @param <T> the Bukkit serializable type
 */
public final class BukkitCodec<T extends ConfigurationSerializable> implements TypeCodec<T> {

    private final Class<T> clazz;

    public BukkitCodec(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public @Nullable T decode(ConfigurationSection section, String path, Type type, @Nullable T defaultValue) {
        Object raw = section.get(path);
        return decodeFromRaw(raw, type, defaultValue);
    }

    @Override
    public T decodeFromRaw(@Nullable Object raw, @NotNull Type type, @Nullable T defaultValue) {
        if (clazz.isInstance(raw)) {
            return clazz.cast(raw);
        }
        return defaultValue;
    }

    @Override
    public @Nullable Object encode(T value) {
        return value;
    }

}
