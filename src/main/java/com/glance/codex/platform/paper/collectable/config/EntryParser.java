package com.glance.codex.platform.paper.collectable.config;

import com.glance.codex.platform.paper.api.collectable.type.CollectableType;
import com.glance.codex.platform.paper.collectable.type.CollectableTypeRegistry;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

@Slf4j
public final class EntryParser {

    @FunctionalInterface
    public interface InstanceFactory<T> {
        /** Return a fresh instance for (id, section); null => skip this entry */
        @Nullable T create(
            @NotNull String id,
            @NotNull ConfigurationSection section,
            @Nullable Injector injector
        ) throws NoSuchMethodException;
    }

    public static <T> Map<String, T> parseAndPopulate(
        @Nullable ConfigurationSection entriesSection,
        InstanceFactory<T> factory,
        BiFunction<T, ConfigurationSection, Boolean> binder,
        @Nullable Injector injector
    ) {
        if (entriesSection == null) return Collections.emptyMap();
        Map<String, T> out = new LinkedHashMap<>();

        for (String id : entriesSection.getKeys(false)) {
            ConfigurationSection child = entriesSection.getConfigurationSection(id);
            if (child == null) continue;
            try {
                T instance = factory.create(id, child, injector);
                if (instance == null) continue;

                if (injector != null) injector.injectMembers(instance);

                binder.apply(instance, child);

                out.put(id, instance);
            } catch (Exception e) {
                log.warn("Failed to populate entry '{}' - '{}'", id, child.getCurrentPath());
            }
        }

        return out;
    }

    @SuppressWarnings("unchecked")
    public static <T> InstanceFactory<T> getFactory(
            @NotNull CollectableTypeRegistry typeRegistry,
            @Nullable String typePath
    ) {
        String path = typePath == null ? "type" : typePath;
        return (id, section, injector) -> {
            String typeId = Optional.ofNullable(section.getString(path)).orElse("base").trim();
            if (typeId.isEmpty()) return null;

            CollectableType cType = typeRegistry.getOr(typeId, "base").orElse(null);
            if (cType == null) return null;

            Class<? extends T> clazz = (Class<? extends T>) cType.type();

            if (injector != null) {
                try {
                    return injector.getInstance(clazz);
                } catch (ConfigurationException | ProvisionException ignored) {}
            }

            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                return null;
            }
        };
    }

}
