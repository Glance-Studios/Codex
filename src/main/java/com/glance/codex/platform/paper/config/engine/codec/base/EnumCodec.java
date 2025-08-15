package com.glance.codex.platform.paper.config.engine.codec.base;

import com.glance.codex.platform.paper.config.engine.codec.TypeCodec;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Codec for Java {@link Enum} types
 * <p>
 * Supports case-insensitive string matching based on {@code name()}
 * <p>
 * Values that do not match any constant will return {@code defaultValue}
 * <p>
 * TODO: enum alias support
 *
 * @author Cammy
 */
public final class EnumCodec implements TypeCodec<Enum<?>> {

    @Override
    public @Nullable Enum<?> decode(ConfigurationSection section, String path, Type type, @Nullable Enum<?> defaultValue) {
        String raw = section.getString(path);
        return decodeFromRaw(raw, type, defaultValue);
    }

    @Override
    public Enum<?> decodeFromRaw(@Nullable Object raw, @NotNull Type type, @Nullable Enum<?> defaultValue) {
        if (!(type instanceof Class<?> cls) || !cls.isEnum()) {
            return defaultValue;
        }

        for (Object constant : cls.getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(String.valueOf(raw))) {
                return (Enum<?>) constant;
            }
        }

        return defaultValue;
    }

    @Override
    public @Nullable Object encode(Enum<?> value) {
        return value.name();
    }

}
