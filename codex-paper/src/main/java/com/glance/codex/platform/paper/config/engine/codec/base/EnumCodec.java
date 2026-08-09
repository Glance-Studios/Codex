package com.glance.codex.platform.paper.config.engine.codec.base;

import com.glance.codex.utils.data.TypeCodec;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

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
@Slf4j
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

        // An absent key legitimately falls back to the default, but a value that was written
        // and matched nothing is a typo. Silently defaulting it hides real config mistakes,
        // e.g. a misspelled PLAYER quietly running a command as console instead.
        if (raw != null) {
            log.warn("'{}' is not a valid {} value, using {} instead. Valid values: {}",
                    raw,
                    cls.getSimpleName(),
                    defaultValue,
                    Arrays.stream(cls.getEnumConstants())
                            .map(c -> ((Enum<?>) c).name())
                            .collect(Collectors.joining(", ")));
        }

        return defaultValue;
    }

    @Override
    public @Nullable Object encode(Enum<?> value) {
        return value.name();
    }

}
