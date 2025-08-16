package com.glance.codex.platform.paper.config.engine.codec.base;

import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import com.glance.codex.utils.data.TypeCodec;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Codec for custom {@link ConfigSerializable} types
 * <p>
 * Supports both POJO and record config beans
 * <p>
 * TODO:
 * <li>Kotlin data classes</li>
 * <li>Move methods from ConfigSerializable into this codec for centralized logic</li>
 *
 * @param <T> the user-defined config serializable type
 * @author Cammy
 */
@Slf4j
public final class ConfigSerializableCodec<T extends ConfigSerializable> implements TypeCodec<T> {

    private final Class<T> clazz;

    public ConfigSerializableCodec(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public @Nullable T decode(ConfigurationSection section, String path, Type type, @Nullable T defaultValue) {
        Object raw = section.get(path);
        if (raw == null) {
            return defaultValue;
        }

        return decodeFromRaw(raw, type, defaultValue);
    }

    @Override
    public T decodeFromRaw(@Nullable Object raw, @NotNull Type type, @Nullable T defaultValue) {
        if (raw == null) {
            return defaultValue;
        }

        try {
            return ConfigSerializable.deserialize(raw, clazz);
        } catch (Exception e) {
            log.error("Failed to decode config serializable class '{}' from raw data: '{}'. Falling back to default", clazz.getName(), raw, e);
            return defaultValue;
        }
    }

    @Override
    public @Nullable Object encode(T value) {
        return (value != null) ? value.serialize() : null;
    }

}
