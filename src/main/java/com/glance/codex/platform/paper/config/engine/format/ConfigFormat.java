package com.glance.codex.platform.paper.config.engine.format;

import org.bukkit.configuration.ConfigurationSection;

import java.io.File;

/**
 * Represents a format handler for reading and writing config files in various formats
 * such as YAML, JSON, TXT, etc
 * <p>
 * Implementations are responsible for converting raw files into {@link ConfigurationSection}s
 * and writing those sections back to disk
 *
 * @author Cammy
 */
public interface ConfigFormat {

    /** Load the file into a Bukkit ConfigurationSection (root or named section) */
    ConfigurationSection load(File file, String sectionPath) throws Exception;
    /** Save the section (or entire file) back to disk */
    void save(ConfigurationSection section, File file) throws Exception;

    /**
     * Represents a supported config file format with associated file extensions
     */
    enum Type {
        RAW(""),
        YAML("yml", "yaml"),
        JSON("json"),
        TEXT("txt"),
        PROPERTIES("properties");

        private final String[] exts;
        Type(String... exts) { this.exts = exts; }
        public String[] getExtensions() { return exts; }
    }

}
