package com.glance.codex.platform.paper.config.engine.codec.base;

import com.glance.codex.platform.paper.config.engine.codec.CodecRegistry;
import com.glance.codex.platform.paper.config.engine.codec.ConfigInterpolator;
import com.glance.codex.platform.paper.config.engine.codec.TypeCodec;
import lombok.experimental.UtilityClass;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.BiFunction;

@ApiStatus.Internal
@UtilityClass
public class PrimitiveCodecs {

    private static final Logger log = LoggerFactory.getLogger(PrimitiveCodecs.class);

    /**
     * Registers all standard primitive and boxed types into the given codec registry
     */
    public void registerAll() {
        CodecRegistry.register(Integer.class, new PrimitiveCodec<>(ConfigurationSection::getInt,
                int.class, Integer.class));

        CodecRegistry.register(Long.class, new PrimitiveCodec<>(ConfigurationSection::getLong,
                long.class, Long.class));

        CodecRegistry.register(Double.class, new PrimitiveCodec<>(ConfigurationSection::getDouble,
                double.class, Double.class));

        CodecRegistry.register(Float.class, new PrimitiveCodec<>(
                (sec, path) -> (float) sec.getDouble(path),
                float.class, Float.class
        ));

        CodecRegistry.register(Boolean.class, new PrimitiveCodec<>(ConfigurationSection::getBoolean,
                boolean.class, Boolean.class));

        CodecRegistry.register(Byte.class, new PrimitiveCodec<>(
                (sec, path) -> (byte) sec.getInt(path),
                byte.class, Byte.class
        ));

        CodecRegistry.register(Short.class, new PrimitiveCodec<>(
                (sec, path) -> (short) sec.getInt(path),
                short.class, Short.class
        ));

        CodecRegistry.register(Character.class, new PrimitiveCodec<>(
                (sec, path) -> {
                    String s = sec.getString(path);
                    return (s != null && s.length() == 1) ? s.charAt(0) : null;
                },
                char.class, Character.class
        ));

        CodecRegistry.register(String.class, new PrimitiveCodec<>(
                (sec, path) -> {
                    String s = sec.getString(path);
                    return (s != null) ? ConfigInterpolator.interpolate(s) : null;
                },
                String.class
        ));
    }

    /**
     * A basic codec for primitive or boxed types
     *
     * @param <T> the primitive/boxed type
     */
    @ApiStatus.Internal
    public static class PrimitiveCodec<T> implements TypeCodec<T> {

        private final Set<Class<?>> supportedTypes;
        private final BiFunction<ConfigurationSection, String, T> reader;

        public PrimitiveCodec(
                @NotNull final BiFunction<ConfigurationSection, String, T> reader,
                Class<?>... supportedTypes
        ) {
            this.supportedTypes = Set.of(supportedTypes);
            this.reader = reader;
        }

        @Override
        public @Nullable T decode(ConfigurationSection section, String path, Type type, @Nullable T defaultValue) {
            try {
                T value = reader.apply(section, path);
                return value != null ? value : defaultValue;
            } catch (Exception e) {
                log.error("Failed to decode type '{}' from path: '{}'. Falling back to default", type, path, e);
                return defaultValue;
            }
        }

        @Override
        public @Nullable Object encode(T value) {
            return value;
        }

        public boolean supports(Type type) {
            return type instanceof Class<?> cls && supportedTypes.contains(cls);
        }

    }

}
