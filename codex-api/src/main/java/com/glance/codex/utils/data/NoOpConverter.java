package com.glance.codex.utils.data;

/**
 * Default no-op converter
 */
public class NoOpConverter implements TypeConverter<Object> {
    @Override
    public Object serialize(Object value) {
        return value;
    }

    @Override
    public Object deserialize(Object raw) {
        return null;
    }
}
