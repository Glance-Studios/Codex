package com.glance.codex.utils.data;

/**
 * Transforms between type T and a serialized representation (Map, String, List, etc.)
 */
public interface TypeConverter<T> {
    /**
     * Convert from a Java object into something the serialized system can handle (Map, List, String, Number, Boolean)
     */
    Object serialize(T value);
    /**
     * Convert from the raw serialized value (Object returned by section.get(...)) back into T
     * <p>
     * Return null to signal "I can’t handle it, fall back to default logic."
     */
    T deserialize(Object raw);
}
