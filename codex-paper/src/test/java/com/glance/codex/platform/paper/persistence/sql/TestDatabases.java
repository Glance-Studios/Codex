package com.glance.codex.platform.paper.persistence.sql;

import com.glance.codex.platform.paper.persistence.config.SqlBootstrap;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Where the dialect tests get their connections. */
public final class TestDatabases {

    static final String URL = "CODEX_TEST_MARIADB_URL";
    static final String USER = "CODEX_TEST_MARIADB_USER";
    static final String PASSWORD = "CODEX_TEST_MARIADB_PASSWORD";

    private TestDatabases() {
    }

    /**
     * Skips rather than fails when no server is named, since the usual build has none.
     *
     * @return a connection to the throwaway MariaDB named by the environment
     */
    public static SqlBootstrap mariaDb() {
        String url = System.getenv(URL);
        assumeTrue(url != null && !url.isBlank(), "Set " + URL + " to run the MariaDB tests");

        return new SqlBootstrap(
            url,
            System.getenv().getOrDefault(USER, ""),
            System.getenv().getOrDefault(PASSWORD, ""),
            2,
            1
        );
    }

    public static SqlBootstrap sqlite(java.nio.file.Path file) {
        return new SqlBootstrap("jdbc:sqlite:" + file, "", "", 1, 1);
    }
}
