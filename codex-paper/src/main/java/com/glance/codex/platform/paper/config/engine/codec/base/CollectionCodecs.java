package com.glance.codex.platform.paper.config.engine.codec.base;

import com.glance.codex.platform.paper.config.engine.codec.TypeCodec;
import com.glance.codex.utils.ReflectionUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Provides configuration codecs for common {@link Collection} types
 *
 * @author Cammy
 */
public final class CollectionCodecs {

    /**
     * Abstract base codec for decoding {@link Collection} types
     * <p>
     * Subclasses must provide a {@link #newCollection()} implementation
     * to create an appropriate collection instance (e.g., {@link ArrayList}, {@link LinkedHashSet})
     *
     * @param <T> the element type
     * @param <C> the concrete collection type
     */
    public abstract static class CollectionCodec<T, C extends Collection<T>> implements TypeCodec<C> {

        protected final TypeCodec<T> elementCodec;

        protected CollectionCodec(TypeCodec<T> elementCodec) {
            this.elementCodec = elementCodec;
        }

        @Override
        public Object encode(C value) {
            return value.stream()
                    .map(elementCodec::encode)
                    .toList();
        }

        @Override
        public C decodeFromRaw(@Nullable Object raw, @NotNull Type type, @Nullable C defaultValue) {
            if (!(raw instanceof List<?> list)) return defaultValue;

            C result = newCollection();
            for (Object item : list) {
                Type itemType = ReflectionUtils.extractTypeArgument(type, 0, Object.class);
                T val = elementCodec.decodeFromRaw(item, itemType, null);
                if (val != null) result.add(val);
            }

            return result;
        }

        @Override
        public @Nullable C decode(ConfigurationSection section, String path, Type type, @Nullable C defaultValue) {
            List<?> rawList = section.getList(path);
            if (rawList == null) return defaultValue;

            C result = newCollection();
            for (Object rawElement : rawList) {
                // Decode using the element codec
                Type itemType = ReflectionUtils.extractTypeArgument(type, 0, Object.class);
                T decoded = elementCodec.decodeFromRaw(rawElement, itemType, null);
                if (decoded != null) result.add(decoded);
            }

            return result;
        }

        /**
         * Returns an empty collection instance of the target type
         */
        protected abstract C newCollection();

    }

    /**
     * Codec for {@link List} values
     * <p>
     * Deserializes to {@link ArrayList}
     *
     * @param <T> element type
     */
    public static class ListCodec<T> extends CollectionCodec<T, List<T>> {

        public ListCodec(TypeCodec<T> elementCodec) {
            super(elementCodec);
        }

        @Override
        protected List<T> newCollection() {
            return new ArrayList<>();
        }

    }

    /**
     * Codec for {@link Set} values
     * <p>
     * Deserializes to {@link LinkedHashSet} to preserve order
     *
     * @param <T> element type
     */
    public static class SetCodec<T> extends CollectionCodec<T, Set<T>> {

        public SetCodec(TypeCodec<T> elementCodec) {
            super(elementCodec);
        }

        @Override
        protected Set<T> newCollection() {
            return new LinkedHashSet<>();
        }
    }

}
