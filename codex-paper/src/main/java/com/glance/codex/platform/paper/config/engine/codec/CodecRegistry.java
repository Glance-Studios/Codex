package com.glance.codex.platform.paper.config.engine.codec;

import com.glance.codex.platform.paper.config.engine.codec.base.CollectionCodecs;
import com.glance.codex.platform.paper.config.engine.codec.base.StringMapCodec;
import com.google.common.reflect.TypeToken;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Central registry and resolver for {@link TypeCodec} instances used in config serialization
 * <p>
 * Supports:
 * <ul>
 *     <li>Direct registration of codecs for raw types (e.g. {@code String.class})</li>
 *     <li>Registration of codecs for generic {@link TypeToken} (e.g. {@code List<String>})</li>
 *     <li>Built iin resolution for common generic type like {@code List<T>}, {@code Set<T>}</li>
 *     <li>Dynamic resolution via {@link CodecResolver}s</li>
 * </ul>
 *
 * @author Cammy
 */
@Slf4j
@UtilityClass
public class CodecRegistry {

    private final Map<TypeToken<?>, TypeCodec<?>> codecs = new HashMap<>();
    private final List<CodecResolver> dynamicResolvers = new ArrayList<>();

    /**
     * Register a codec by exact type (e.g., <code>List&lt;T&gt;</code>)
     */
    public <T> void register(@NotNull TypeToken<T> token, @NotNull TypeCodec<T> codec) {
        codecs.put(token, codec);
    }

    /**
     * Register a codec for a raw class (e.g., <code>String, int.class</code>)
     */
    public <T> void register(@NotNull Class<T> cls, @NotNull TypeCodec<T> codec) {
        codecs.put(TypeToken.of(cls), codec);
    }

    /**
     * Registers a dynamic resolver to resolve the codec on the fly
     * @param resolver to resolve the codec
     */
    public void registerResolver(@NotNull CodecResolver resolver) {
        dynamicResolvers.add(resolver);
    }

    /**
     * Tries to resolve a codec for any generic or raw type
     */
    public @Nullable TypeCodec<?> find(Type type) {
        TypeToken<?> token = TypeToken.of(type);

        // 1. Exact match
        TypeCodec<?> match = codecs.get(token);
        if (match != null) return match;

        // 2. Parameterized fallback (built in support)
        if (type instanceof ParameterizedType pt) {
            match = resolveParameterized(token, pt);
            if (match != null) return match;
        }

        // 3. Dynamic match
        for (CodecResolver resolver : dynamicResolvers) {
            TypeCodec<?> dynamic = resolver.resolve(type);
            if (dynamic != null) return dynamic;
        }

        // 4. Supporting
        for (TypeCodec<?> codec : codecs.values()) {
            if (codec.supports(type)) {
                return codec;
            }
        }

        // 5. Nothing matched
        log.debug("Type: {} had no codec found", type.getTypeName());
        return null;
    }

    /**
     * Attempts to resolve a {@link TypeCodec} for a supported {@link ParameterizedType},
     * such as {@code List<T>} by recursively resolving
     * the codecs for its generic type arguments
     * <p>
     * Currently, supports:
     * <ul>
     *   <li>{@code List<T>}</li>
     *   <li>{@code Set<T>}</li>
     *   <li>{@code Map<String, V>}</li>
     * </ul>
     *
     * @param token the full type token for the parameterized type (reserved for future use)
     * @param pType the raw {@link ParameterizedType} to resolve
     * @return a resolved {@link TypeCodec}, or {@code null} if the type is unsupported or unresolvable
     */
    private @Nullable TypeCodec<?> resolveParameterized(TypeToken<?> token, ParameterizedType pType) {
        Type raw = pType.getRawType();
        if (!(raw instanceof Class<?> rawClass)) return null;

        Type[] args = pType.getActualTypeArguments();

        if (List.class.isAssignableFrom(rawClass) && args.length == 1) {
            TypeCodec<?> element = find(args[0]);
            return (element != null) ? new CollectionCodecs.ListCodec<>(element) : null;
        }

        if (Set.class.isAssignableFrom(rawClass) && args.length == 1) {
            TypeCodec<?> element = find(args[0]);
            return (element != null) ? new CollectionCodecs.SetCodec<>(element) : null;
        }

        if (Map.class.isAssignableFrom(rawClass) && args.length == 2) {
            TypeCodec<?> value = find(args[1]);
            if (args[0] == String.class) {
                return (value != null) ? new StringMapCodec<>(value) : null;
            }
        }

        return null;
    }

}
