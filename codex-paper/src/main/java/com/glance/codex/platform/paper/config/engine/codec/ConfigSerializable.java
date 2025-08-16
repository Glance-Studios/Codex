package com.glance.codex.platform.paper.config.engine.codec;

import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigPath;
import com.glance.codex.utils.data.TypeCodec;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Marker for types that can be serialized to and from config-compatible structures
 * <p>
 * Classes implementing this will be auto-serialized into YAML sections or maps,
 * and deserialized vis reflection or record constructor
 * <p>
 * TODO: Move complex inner logic here to codec to generalize all serialization
 *
 * @author Cammy
 */
public interface ConfigSerializable {

    /**
     * Serializes this object into a Map<String, Object> compatible with Bukkit's config structure
     * <p>
     * Fields annotated with {@link ConfigField} will be prioritized by order if present,
     * otherwise, all declared fields are serialized
     *
     * @return the serialized field map
     */
    default Map<String, Object> serialize() {
        Class<?> cls = this.getClass();

        List<Field> toSerialize = getConfigurableFields(cls);

        toSerialize.sort(Comparator.comparingInt(f -> f.getAnnotation(ConfigField.class) != null
                ? f.getAnnotation(ConfigField.class).order()
                : Integer.MAX_VALUE
        ));

        Map<String, Object> map = new LinkedHashMap<>();
        for (Field f : toSerialize) {
            f.setAccessible(true);
            try {
                Object value = f.get(this);
                if (value != null) {
                    map.put(f.getName(), toConfigCompatible(value));
                }
            } catch (Exception e) {
                // ignore inaccessible
            }
        }

        return map;
    }

    /**
     * Deserializes an object from the given raw value (map or ConfigurationSection)
     * <p>
     * Automatically handles record, POJOs, supports nested deserialization
     *
     * @param rawValue the raw data
     * @param cls the type to instantiate
     * @return the deserialized object
     * @throws Exception if the deserialization fails
     */
    @SuppressWarnings("unchecked")
    static <T extends ConfigSerializable> T deserialize(
            @NotNull Object rawValue,
            @NotNull Class<T> cls
    ) throws Exception {
        if (cls.isRecord()) {
            return (T) deserializeRecord(rawValue, cls);
        }

        // Requesting an api class
        if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers())) {
            throw new UnsupportedOperationException("Interface resolvers are not currently supported: " + cls.getName());
        }

        if (rawValue instanceof ConfigurationSection section) {
            return deserializeFromSection(section, cls);
        }
        if (rawValue instanceof Map<?, ?> map) {
            return deserializeFromMap(map, cls);
        }
        throw new IllegalArgumentException("Cannot deserialize " + cls.getSimpleName() + " from " + rawValue);
    }

    /**
     * Invokes a record’s canonical constructor with values read from
     * either a ConfigurationSection or Map
     */
    @SuppressWarnings("unchecked")
    private static Object deserializeRecord(Object rawValue, Class<?> recordClass) throws Exception {
        // Turn our rawValue into a simple key - value Map
        Map<String, Object> source = new LinkedHashMap<>();
        if (rawValue instanceof ConfigurationSection sec) {
            for (String key : sec.getKeys(false)) {
                source.put(key, sec.get(key));
            }
        } else if (rawValue instanceof Map<?,?> m) {
            //noinspection unchecked
            source.putAll((Map<String, Object>)m);
        } else {
            throw new IllegalArgumentException(
                    "Cannot deserialize record " + recordClass.getSimpleName() +
                            " from " + rawValue.getClass().getSimpleName()
            );
        }

        // Grab the record components (in declaration order)
        var components = recordClass.getRecordComponents();
        Object[] args  = new Object[components.length];
        Class<?>[] types = new Class<?>[components.length];
        ConfigurationSection section = new MapBackedSection(source);

        // For each component: pull the @ConfigField name (or default to component.getName()),
        // fetch via ConfigReader, and store into args[]
        for (int i = 0; i < components.length; i++) {
            var comp      = components[i];
            String name   = comp.getName();
            ConfigPath cp = comp.getAnnotation(ConfigPath.class);
            String path   = (cp != null && !cp.value().isEmpty()) ? cp.value() : name;

            Type type = comp.getGenericType();
            TypeCodec<Object> codec = (TypeCodec<Object>) CodecRegistry.find(type);

            Object value = (codec != null)
                    ? codec.decode(section, path, type, null)
                    : null;

            args[i] = value;
            types[i] = comp.getType();
        }

        // find and invoke the canonical constructor
        var ctor = recordClass.getDeclaredConstructor(types);

        ctor.setAccessible(true);
        return ctor.newInstance(args);
    }

    /**
     * Instantiates and populates a POJO-style {@link ConfigSerializable} from a ConfigurationSection
     *
     * @param section the source section
     * @param cls the class to instantiate
     * @return the populated instance
     * @throws Exception if any field fails to deserialize
     */
    @SuppressWarnings("unchecked")
    private static <T extends ConfigSerializable> T deserializeFromSection(
            ConfigurationSection section,
            Class<T> cls
    ) throws Exception {
        T instance = cls.getDeclaredConstructor().newInstance();

        for (Field f : getConfigurableFields(cls)) {
            f.setAccessible(true);
            Object defaultVal = f.get(instance);
            Type type = f.getGenericType();

            TypeCodec<Object> codec = (TypeCodec<Object>) CodecRegistry.find(type);
            Object value = (codec != null)
                    ? codec.decode(section, f.getName(), type, defaultVal)
                    : defaultVal;

            f.set(instance, value);
        }

        return instance;
    }

    /**
     * Instantiates and populates a {@link ConfigSerializable} from a raw map by wrapping it as a section
     *
     * @param map the raw key-value data
     * @param cls the class to instantiate
     * @return the populated instance
     * @throws Exception if any field fails to deserialize
     */
    @SuppressWarnings("unchecked")
    private static <T extends ConfigSerializable> T deserializeFromMap(
            Map<?, ?> map,
            Class<T> cls
    ) throws Exception {
        return deserializeFromSection(new MapBackedSection((Map<String, Object>) map), cls);
    }

    /**
     * Returns the list of fields to be serialized for the given class
     * <p>
     * If fields annotated with {@link ConfigField} exist, only those are used,
     * otherwise, all declared fields are returned
     *
     * @param cls the class to inspect
     * @return list of serializable fields
     */
    private static List<Field> getConfigurableFields(Class<?> cls) {
        Field[] all = cls.getDeclaredFields();

        List<Field> toSerialize = Arrays.stream(all)
                .filter(f -> f.isAnnotationPresent(ConfigField.class))
                // collect into a mutable ArrayList
                .collect(Collectors.toCollection(ArrayList::new));

        if (toSerialize.isEmpty()) {
            toSerialize = new ArrayList<>(Arrays.asList(all));
        }

        return toSerialize;
    }

    /**
     * Convert any supported object into a config-compatible value:
     * <li>primitives, String, enum, etc -> as is</li>
     * <li>ConfigSerializable -> Map via serialize()</li>
     * <li>Collection -> List of config-compatible elements</li>
     * <li>Map -> {@code Map<String, Object>} of config-compatible values</li>
     */
    static Object toConfigCompatible(Object raw) {
        if (raw instanceof ConfigSerializable cs) {
            return cs.serialize();
        }
        if (raw instanceof Collection<?> coll) {
            return coll.stream()
                    .map(ConfigSerializable::toConfigCompatible)
                    .toList();
        }
        if (raw instanceof Map<?, ?> m) {
            return m.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> toConfigCompatible(e.getValue()),
                            (a, b) -> b,
                            LinkedHashMap::new
                    ));
        }
        if (raw instanceof Enum<?> e) {
            return e.name();
        }
        if (raw instanceof Character c) {
            return c.toString();
        }

        return raw;
    }

}
