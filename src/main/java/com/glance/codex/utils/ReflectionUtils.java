package com.glance.codex.utils;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@UtilityClass
public class ReflectionUtils {

    public <T> T instantiate(Class<? extends T> cls) {
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + cls, e);
        }
    }

    public Object getFieldValue(Field f, Object instance) {
        try {
            return f.get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void setFieldValue(Field f, Object instance, Object value) {
        try {
            f.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the N-th generic type argument from the given {@link Type}, or returns {@code fallback} if not present
     *
     * @param type the generic type, e.g. List&lt;T&gt;, Map&lt;K, V&gt;
     * @param index the position of the type argument (0 for first, etc.)
     * @param fallback the fallback if extraction fails
     * @return the extracted type argument, or fallback
     */
    public Type extractTypeArgument(Type type, int index, Type fallback) {
        if (type instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (index >= 0 && index < args.length) {
                return args[index];
            }
        }
        return fallback;
    }

}
