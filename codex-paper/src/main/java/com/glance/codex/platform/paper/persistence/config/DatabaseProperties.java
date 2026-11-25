package com.glance.codex.platform.paper.persistence.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Connection details read from a database.properties beside config.yml.
 *
 * Kept out of config.yml so a deployment can ship the plugin's settings while leaving the
 * credentials on the server.
 */
public record DatabaseProperties(
    @NotNull String host,
    int port,
    @NotNull String database,
    @NotNull String user,
    @NotNull String password,
    int minPool,
    int maxPool
) {

    public static final String FILE_NAME = "database.properties";

    /** @return null when the file is absent, so the caller can fall back to config.yml */
    public static @Nullable DatabaseProperties read(@NotNull File dataFolder) throws IOException {
        File file = new File(dataFolder, FILE_NAME);
        if (!file.isFile()) return null;

        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        }

        return new DatabaseProperties(
            required(props, "db.host"),
            Integer.parseInt(required(props, "db.port")),
            required(props, "db.database"),
            required(props, "db.user"),
            required(props, "db.password"),
            number(props, "db.pool.min", 2),
            number(props, "db.pool.max", 8)
        );
    }

    public @NotNull String jdbcUrl() {
        return "jdbc:mariadb://" + host + ":" + port + "/" + database;
    }

    private static @NotNull String required(@NotNull Properties props, @NotNull String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(FILE_NAME + " is missing " + key);
        }
        return value.trim();
    }

    private static int number(@NotNull Properties props, @NotNull String key, int fallback) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) return fallback;
        return Integer.parseInt(value.trim());
    }
}
