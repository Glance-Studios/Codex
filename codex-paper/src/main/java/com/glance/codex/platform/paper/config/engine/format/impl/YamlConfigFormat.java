package com.glance.codex.platform.paper.config.engine.format.impl;

import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import com.glance.codex.platform.paper.config.engine.format.ConfigFormat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public class YamlConfigFormat implements ConfigFormat {
    @Override
    public ConfigurationSection load(File file, String sectionPath) throws Exception {
        // load the root YAML
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        // no section -> return the whole file
        if (sectionPath == null || sectionPath.isEmpty()) {
            return yaml;
        }

        // if subsection exists, return it; otherwise create & return it
        ConfigurationSection sub = yaml.getConfigurationSection(sectionPath);
        return Objects.requireNonNullElseGet(sub, () -> yaml.createSection(sectionPath));
    }

    @Override
    public void save(ConfigurationSection section, File file) throws IOException {
        YamlConfiguration root = getYamlConfiguration(section);

        try {
            root.save(file);
        } catch (YAMLException ye) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save config '" + file.getName() +
                    "': encountered an unsupported value.\n" +
                    "All values in your @Config beans must be one of:\n" +
                    "  - Primitives (int, boolean, double, etc.) or their wrappers\n" +
                    "  - String or Character\n" +
                    "  - Enums\n" +
                    "  - java.util.List / java.util.Set of supported types\n" +
                    "  - java.util.Map<String, ?> of supported types\n" +
                    "  - org.bukkit.configuration.codec.ConfigurationSerializable\n" +
                    "  - Implementations of " + ConfigSerializable.class.getName() + "\n" +
                    "Please check your @Config classes and ensure every field's type\n" +
                    "is supported or has a custom codec.", ye);

            throw ye;
        }
    }

    private @NotNull YamlConfiguration getYamlConfiguration(ConfigurationSection section) {
        YamlConfiguration root;
        if (section instanceof YamlConfiguration yc) {
            // we loaded the full file, so save it directly
            root = yc;
        } else {
            // we loaded only a subsection, grab its root and save that
            root = (YamlConfiguration) section.getRoot();
            if (root == null) {
                throw new IllegalArgumentException(
                        "Cannot save section without YamlConfiguration root"
                );
            }
        }
        return root;
    }

}
