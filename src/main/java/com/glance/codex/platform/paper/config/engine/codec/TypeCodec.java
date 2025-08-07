package com.glance.codex.platform.paper.config.engine.codec;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Represents a serializer/deserializer for a specific type T
 * to and from {@link ConfigurationSection}
 * <p>
 * Implementations define how to read values from config, convert raw instances,
 * and write values in a config-compatible format
 *
 * <p>All codecs should handle null safety and fallbacks via {@code defaultValue}</p>
 *
 * @param <T> the type being serialized/deserialized
 * @author Cammy
 */
public interface TypeCodec<T> {

    /**
     * Decode a value from the configuration
     *
     * @param section the section to read from
     * @param path value path
     * @param type the target generic type
     * @param defaultValue fallback value
     * @return the deserialized object
     */
    @Nullable
    T decode(ConfigurationSection section, String path, Type type, @Nullable T defaultValue);

    /**
     * Decode a value directly from a raw object (e.g. {@code Map}, {@code String}, etc)
     * <p>By default, this attempts a safe cast. Override to implement raw deserialization</p>
     *
     * @param raw the raw object
     * @param type the target generic type
     * @param defaultValue fallback value if decoding fails
     * @return the decoded object or fallback
     */
    default T decodeFromRaw(@Nullable Object raw, @NotNull Type type, @Nullable T defaultValue) {
        // Default decode from raw - assume already type of T
        @SuppressWarnings("unchecked")
        T casted = (T) raw;
        return casted != null ? casted : defaultValue;
    }

    /**
     * Encode an object int a config-compatible format
     *
     * @param value the value to encode
     * @return a value compatible with {@link ConfigurationSection#set}...
     */
    @Nullable
    Object encode(T value);

    /**
     * Check if this codec supports other types
     *
     * @param type a type that shouldn't be T
     * @return true if this codec also supports it
     */
    default boolean supports(Type type) {
        return false;
    }

}
