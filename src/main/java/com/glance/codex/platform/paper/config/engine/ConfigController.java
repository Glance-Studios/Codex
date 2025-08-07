package com.glance.codex.platform.paper.config.engine;

import com.glance.codex.platform.paper.CodexPlugin;
import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigPath;
import com.glance.codex.platform.paper.config.engine.codec.CodecRegistry;
import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import com.glance.codex.platform.paper.config.engine.codec.TypeCodec;
import com.glance.codex.platform.paper.config.engine.codec.base.BukkitCodec;
import com.glance.codex.platform.paper.config.engine.codec.base.ConfigSerializableCodec;
import com.glance.codex.platform.paper.config.engine.codec.base.EnumCodec;
import com.glance.codex.platform.paper.config.engine.codec.base.PrimitiveCodecs;
import com.glance.codex.platform.paper.config.engine.event.ConfigLoadEvent;
import com.glance.codex.platform.paper.config.engine.event.ConfigPushEvent;
import com.glance.codex.platform.paper.config.engine.event.ConfigReloadEvent;
import com.glance.codex.platform.paper.config.engine.event.ConfigSaveEvent;
import com.glance.codex.platform.paper.config.engine.exception.ConfigLoadException;
import com.glance.codex.platform.paper.config.engine.exception.ConfigSaveException;
import com.glance.codex.platform.paper.config.engine.exception.ConfigValidationException;
import com.glance.codex.platform.paper.config.engine.format.ConfigFormat;
import com.glance.codex.platform.paper.config.engine.format.impl.YamlConfigFormat;
import com.glance.codex.utils.ReflectionUtils;
import com.glance.codex.utils.data.TypeConverter;
import com.glance.codex.utils.data.Validator;
import com.glance.codex.utils.io.FileUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central lifecycle loader, and codec bridge for config beans using {@link Config} and {@link ConfigPath}
 * <p>
 * Configs that load through this must be annotated with {@code Config} and implement {@link Config.Handler}
 *
 * @author Cammy
 */
@ApiStatus.Internal
public final class ConfigController {

    // Utility Class
    private ConfigController() {}

    private final static Map<Class<?>, ConfigurationSection> loadedSections = new HashMap<>();
    private final static Map<Class<?>, File> configFiles = new HashMap<>();

    private final static Map<ConfigFormat.Type, ConfigFormat> formatHandlers = Map.of(
            ConfigFormat.Type.YAML, new YamlConfigFormat()
    );

    @SuppressWarnings("unchecked")
    public static void init() {
        PrimitiveCodecs.registerAll();

        CodecRegistry.registerResolver(type -> {
            if (type instanceof Class<?> cls && cls.isEnum()) {
                return new EnumCodec();
            }
            return null;
        });

        CodecRegistry.registerResolver(type -> {
            if (type instanceof Class<?> cls) {
                if (ConfigurationSerializable.class.isAssignableFrom(cls)) {
                    return new BukkitCodec<>((Class<ConfigurationSerializable>) cls);
                }
            }
            return null;
        });

        CodecRegistry.registerResolver(type -> {
            if (type instanceof Class<?> cls) {
                if (ConfigSerializable.class.isAssignableFrom(cls)) {
                    return new ConfigSerializableCodec<>((Class<ConfigSerializable>) cls);
                }
            }
            return null;
        });
    }

    /**
     * Loads a config file from the given directory
     *
     *
     * @param configClass Class type of the config
     * @param instance Instance to populate
     * @return The populated instance
     * @param <T> Type extending {@link Config.Handler}
     * @throws ConfigLoadException if the file or section could not be loaded/populated
     */
    public static <T extends Config.Handler> T loadConfig(Plugin plugin, Class<T> configClass, T instance) throws ConfigLoadException {
        return loadConfig(plugin, configClass, instance, plugin.getDataFolder());
    }

    /**
     * Loads a config file relative to the given data folder
     *
     * @param dataFolder The data folder to search for the config file
     * @param configClass Class type of the config
     * @param instance Instance to populate
     * @return The populated instance
     * @param <T> Type extending {@link Config.Handler}
     * @throws ConfigLoadException if the file or section could not be loaded/populated
     */
    public static <T extends Config.Handler> T loadConfig(Plugin plugin, Class<T> configClass, T instance, File dataFolder) {
        Logger logger = plugin.getLogger();
        Config meta = configClass.getAnnotation(Config.class);
        if (meta == null) {
            throw new ConfigLoadException("Missing @Config annotation on " + configClass.getSimpleName());
        }

        ConfigFormat.Type formatType = meta.format();
        ConfigFormat formatHandler = formatHandlers.get(formatType);
        if (formatHandler == null) {
            throw new UnsupportedOperationException("Unsupported config format: " + formatType);
        }

        boolean useDefault = meta.usePluginConfig();

        ConfigurationSection section = null;
        File file = null;

        if (useDefault) {
            try {
                plugin.saveDefaultConfig();
                section = meta.section().isEmpty()
                        ? plugin.getConfig()
                        : plugin.getConfig().getConfigurationSection(meta.section());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("No bundled config.yml found; creating file as fallback");
            }
        }

        if (!useDefault || section == null) {
            String baseName = meta.fileName();
            List<String> exts = List.of(formatType.getExtensions());

            file = FileUtils.findFile(dataFolder, baseName, exts);
            if (file == null) {
                String resourcePath = baseName + "." + exts.getFirst();

                if (plugin.getResource(resourcePath) != null) {
                    plugin.saveResource(resourcePath, false);
                } else {
                    logger.info("No bundled '" + resourcePath + "' to copy; creating empty one");
                }

                file = new File(dataFolder, resourcePath);
                if (!file.exists()) {
                    try {
                        file = FileUtils.ensureResource(dataFolder, resourcePath);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to create config file " + resourcePath, e);
                        return instance;
                    }
                }
            }

            try {
                section = formatHandler.load(file, meta.section());
            } catch (Exception e) {
                throw new ConfigLoadException("Failed to load config section from file: '"
                        + file.getName() + "' | section: " + meta.section(), e);
            }
        }

        configFiles.put(configClass, file);
        loadedSections.put(configClass, section);

        boolean changed = syncFields(instance, section, true, meta.writeDefaults());

        if (changed) {
            writeToDisk(instance, false);
        }

        // Call load event
        new ConfigLoadEvent(configClass, instance, changed);

        return instance;
    }

    /**
     * Updates the given config instance's fields by reading from the
     * associated {@link ConfigurationSection}
     * <p>
     * No values are read from disk; only in-memory data is used
     *
     * @param instance the config bean implementing {@link Config.Handler}
     */
    public static void pull(Config.Handler instance) {
        Class<?> cls = instance.getClass();
        ConfigurationSection section = loadedSections.get(cls);
        if (section == null) {
            throw new IllegalStateException("Config not loaded: " + cls.getSimpleName());
        }

        syncFields(instance, section, true, false); // pull only
    }

    /**
     * Writes the current in-memory field values from the given config bean instance
     * into the associated {@link ConfigurationSection}
     * <p>
     * No values are written to disk
     *
     * @param instance the config bean implementing {@link Config.Handler}
     */
    public static void push(Config.Handler instance) {
        Class<?> cls = instance.getClass();
        ConfigurationSection section = loadedSections.get(cls);
        if (section == null) {
            throw new IllegalStateException("Config not loaded: " + cls.getSimpleName());
        }

        boolean changed = syncFields(instance, section, false, true);

        // Call event
        new ConfigPushEvent(cls, instance, changed).callEvent();
    }

    /**
     * Writes the current {@link ConfigurationSection} into the associated file
     * <p>
     * If {@code updateInMemory} is true, {@link #push(Config.Handler)} is called first to apply instance field values
     *
     * @param instance the config bean associated to the section and file
     * @param updateInMemory whether to call {@link #push(Config.Handler)} before write
     * @throws ConfigSaveException if an error occurs during this process
     */
    public static void writeToDisk(Config.Handler instance, boolean updateInMemory) {
        if (updateInMemory) push(instance);

        Class<?> cls = instance.getClass();
        Config meta = cls.getAnnotation(Config.class);
        if (meta == null) {
            throw new ConfigSaveException("Missing @Config annotation for " + cls.getSimpleName());
        }

        if (meta.usePluginConfig()) {
            CodexPlugin.getInstance().saveConfig();
            return;
        }

        File file = configFiles.get(cls);
        ConfigurationSection section = loadedSections.get(cls);
        ConfigFormat formatHandler = formatHandlers.get(meta.format());

        if (file == null || section == null || formatHandler == null) {
            throw new ConfigSaveException("Unable to save config for " + cls.getSimpleName());
        }

        try {
            formatHandler.save(section, file);
        } catch (Exception e) {
            throw new ConfigSaveException("Failed to save config to " + file.getName(), e);
        }

        // Call event
        new ConfigSaveEvent(cls, instance).callEvent();
    }

    /**
     * Reloads the associated config file values into the instance and its associated
     * {@link ConfigurationSection}
     *
     * @param instance the config bean
     */
    public static void reload(Config.Handler instance) {
        Class<?> cls = instance.getClass();
        Config meta = cls.getAnnotation(Config.class);
        if (meta == null) {
            throw new ConfigLoadException("Missing @Config annotation for " + cls.getSimpleName());
        }

        File file = configFiles.get(cls);
        if (file == null || !file.exists()) {
            throw new ConfigLoadException("No config file found for " + cls.getSimpleName());
        }

        ConfigFormat formatHandler = formatHandlers.get(meta.format());
        if (formatHandler == null) {
            throw new ConfigLoadException("No format handler for " + meta.format());
        }

        try {
            ConfigurationSection section = formatHandler.load(file, meta.section());

            // Compare to old section for change detection
            ConfigurationSection oldSection = loadedSections.get(cls);
            boolean sectionChanged = !Objects.equals(section, oldSection); // might need deeper?

            loadedSections.put(cls, section);

            boolean fieldsChanged = syncFields(instance, section, true, meta.writeDefaults());

            // Call event
            new ConfigReloadEvent(cls, instance, sectionChanged, fieldsChanged).callEvent();
        } catch (Exception e) {
            throw new ConfigLoadException("Failed to reload config for " + cls.getSimpleName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean syncFields(
            Object instance,
            ConfigurationSection section,
            boolean readIntoFields,
            boolean writeDefaults
    ) {
        boolean changed = false;

        Class<?> cls = instance.getClass();

        for (Field field : cls.getDeclaredFields()) {
            ConfigPath cp = field.getAnnotation(ConfigPath.class);
            if (cp == null) continue;

            // TODO: phase out in favor of codec
            @Deprecated
            TypeConverter<Object> converter = (TypeConverter<Object>) ReflectionUtils.instantiate(cp.converter());

            field.setAccessible(true);
            String path = cp.value();
            Object defaultValue = ReflectionUtils.getFieldValue(field, instance);
            Type fieldType = field.getGenericType();

            if (writeDefaults && cp.writeDefault() && !section.contains(path)) {
                Object rawOut = converter.serialize(defaultValue);

                if (rawOut instanceof ConfigSerializable cs) {
                    // bean field write:
                    Map<String, Object> serialized = cs.serialize();
                    for (var e : serialized.entrySet()) {
                        String childKey = e.getKey();
                        Object childValue = e.getValue();
                        String childPath  = path + "." + childKey;

                        // write the value
                        section.set(childPath, ConfigSerializable.toConfigCompatible(childValue));

                        // immediately write the @ConfigField comments, if any
                        try {
                            Field childField = defaultValue.getClass()
                                    .getDeclaredField(childKey);
                            ConfigField fieldAnn = childField.getAnnotation(ConfigField.class);
                            if (fieldAnn != null && fieldAnn.comments().length > 0) {
                                section.setInlineComments(childPath, List.of(fieldAnn.comments()));
                            }
                        } catch (NoSuchFieldException ignored) {
                        }
                    }
                } else {
                    // fallback for primitives, lists, maps, etc
                    section.set(path, ConfigSerializable.toConfigCompatible(rawOut));
                }

                // top-level @ConfigPath comments on the bean itself
                if (cp.comments().length > 0) {
                    section.setInlineComments(path, List.of(cp.comments()));
                }

                changed = true;
            }

            // Skip reading into fields if we're in write-only mode
            if (!readIntoFields) continue;

            // Deserialize config back into the field
            Object raw = section.get(path);
            Object deserialized = converter.deserialize(raw);

            Object converted;
            if (deserialized != null) {
                converted = deserialized;
            } else {
                TypeCodec<Object> codec = (TypeCodec<Object>) CodecRegistry.find(fieldType);
                converted = (codec != null)
                        ? codec.decode(section, path, fieldType, defaultValue)
                        : defaultValue;
            }

            Validator<Object> validator = (Validator<Object>) ReflectionUtils.instantiate(cp.validator());
            try {
                Optional<String> err = validator.validate(converted);
                if (err.isPresent()) {
                    throw new ConfigValidationException(err.get());
                }
            } catch (Exception e) {
                throw new ConfigValidationException(
                        "Validation failed for '" + path +
                                "' in " + cls.getSimpleName() + ": " + e.getMessage()
                );
            }

            // inject
            ReflectionUtils.setFieldValue(field, instance, converted);
        }

        return changed;
    }

    /**
     * Returns an Optional of a loaded ConfigurationSection for the given config class
     */
    public static Optional<ConfigurationSection> getConfigSection(Class<?> cls) {
        return Optional.ofNullable(loadedSections.get(cls));
    }

    /**
     * Returns an Optional of a loaded File for the given config class
     */
    public static Optional<File> getConfigFile(Class<?> cls) {
        return Optional.ofNullable(configFiles.get(cls));
    }

    /**
     * Returns true if the given config class is currently loaded
     */
    public static boolean isConfigLoaded(Class<?> cls) {
        return getConfigSection(cls).isPresent();
    }

    /**
     * Immediately unloads the given config from memory,
     * removing its cached section
     */
    public static void unloadConfig(Class<?> cls) {
        loadedSections.remove(cls);
    }

    public static void unloadAll() {
        loadedSections.clear();
        configFiles.clear();
    }

}

