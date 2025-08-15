package com.glance.codex.platform.paper.config.engine.codec.base;

import com.glance.codex.platform.paper.config.engine.codec.TypeCodec;
import com.glance.codex.utils.ReflectionUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Codec for Map<String, V> types, using a delegated codec for value deserialization
 *
 * @param <V> the value type
 *
 * @author Cammy
 */
public final class StringMapCodec<V> implements TypeCodec<Map<String, V>> {

    private final TypeCodec<V> valueCodec;

    public StringMapCodec(TypeCodec<V> valueCodec) {
        this.valueCodec = valueCodec;
    }

    @Override
    public @Nullable Map<String, V> decode(ConfigurationSection section, String path, Type type, @Nullable Map<String, V> defaultValue) {
        ConfigurationSection sub = section.getConfigurationSection(path);
        if (sub == null) return defaultValue;

        Map<String, V> result = new LinkedHashMap<>();
        for (String key : sub.getKeys(false)) {
            V value = valueCodec.decode(sub, key, null, null);
            if (value != null) result.put(key, value);
        }

        return result;
    }

    @Override
    public Map<String, V> decodeFromRaw(@Nullable Object raw, @NotNull Type type, @Nullable Map<String, V> defaultValue) {
        if (!(raw instanceof Map<?,?> rawMap)) return defaultValue;

        Map<String, V> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            Object rawKey = entry.getKey();
            if (!(rawKey instanceof String key)) continue;

            Type valueType = ReflectionUtils.extractTypeArgument(type, 1, Object.class);
            V value = valueCodec.decodeFromRaw(entry.getValue(), valueType, null);
            if (value != null) result.put(key, value);
        }

        return result;
    }

    @Override
    public @Nullable Object encode(Map<String, V> value) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, V> entry : value.entrySet()) {
            Object encoded = valueCodec.encode(entry.getValue());
            if (encoded != null) {
                out.put(entry.getKey(), encoded);
            }
        }
        return out;
    }

}
