package com.glance.codex.platform.paper.persistence.config;

import com.glance.codex.platform.paper.persistence.sql.CollectableDao;
import com.glance.codex.platform.paper.persistence.sql.MySQLCollectableDao;
import com.glance.codex.platform.paper.persistence.sql.SqliteCollectableDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.plugin.Plugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.io.IOException;

@Singleton
@Getter
@Accessors(fluent = true)
public final class SqlBootstrap implements AutoCloseable {

    private final DataSource dataSource;
    private final Jdbi jdbi;
    private final Dialect dialect;
    private final Class<? extends CollectableDao> daoType;

    public enum Dialect { SQLITE, MYSQL, MARIADB }

    /**
     * Connection details come from database.properties when it is there, and config.yml when it is
     * not, so a deployment can ship settings while leaving the credentials on the server.
     */
    @Inject
    public SqlBootstrap(
        @NotNull Plugin plugin,
        @NotNull CollectableStorageConfig cfg
    ) {
        this(resolve(plugin, cfg));
    }

    private SqlBootstrap(@NotNull Settings settings) {
        this(
            settings.url(),
            settings.user(),
            settings.password(),
            settings.maxPool(),
            settings.minIdle()
        );
    }

    public SqlBootstrap(
        @NotNull String jdbcUrl,
        @NotNull String user,
        @NotNull String password,
        int maxPool,
        int minIdle
    ) {
        this.dialect = dialectOf(jdbcUrl);
        this.daoType = dialect == Dialect.SQLITE
                ? SqliteCollectableDao.class
                : MySQLCollectableDao.class;

        // Before the pool, so a missing driver reads as one rather than as a failed connection
        requireDriver(this.dialect);

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(jdbcUrl);

        if (!user.isEmpty()) hc.setUsername(user);
        if (!password.isEmpty()) hc.setPassword(password);

        hc.setMaximumPoolSize(maxPool);
        hc.setMinimumIdle(minIdle);

        this.dataSource = new HikariDataSource(hc);
        this.jdbi = configure(this.dataSource);

        createSchema(this.daoType);
    }

    /**
     * Builds the Jdbi the storage layer expects.
     *
     * The row carriers hold public fields rather than bean properties or a constructor, so nothing
     * can map a query result until the field mapper is registered for each of them.
     */
    public static Jdbi configure(@NotNull DataSource dataSource) {
        return Jdbi.create(dataSource)
                .installPlugins()
                .installPlugin(new SqlObjectPlugin())
                .registerRowMapper(FieldMapper.factory(CollectableDao.Row.class))
                .registerRowMapper(FieldMapper.factory(CollectableDao.CountRow.class));
    }

    /**
     * Refuses to start rather than letting the storage fail one query at a time.
     *
     * The driver the URL needs is decided by the URL, not by the configured backend name, so this
     * is the only place that can tell which one has to be there.
     */
    private static void requireDriver(@NotNull Dialect dialect) {
        if (dialect == Dialect.SQLITE) {
            if (present("org.sqlite.JDBC")) return;
            throw new IllegalStateException("""
                The connection is for SQLite but no SQLite driver is present.
                Options:
                  - Use the '-sqlite' or '-spellbrook' jar of CollectablesCodex, or
                  - Install a dedicated SQLite driver plugin, or
                  - Switch storage.backend to FLATFILE in config.yml.
                """);
        }

        if (present("org.mariadb.jdbc.Driver") || present("com.mysql.cj.jdbc.Driver")) return;
        throw new IllegalStateException("""
            The connection is for MySQL or MariaDB but neither driver is present.
            The driver is declared as a Paper library, so this usually means the server could not
            reach the repository at startup. Check the earlier library resolution errors.
            """);
    }

    private static boolean present(@NotNull String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static Dialect dialectOf(@NotNull String url) {
        if (url.startsWith("jdbc:sqlite")) return Dialect.SQLITE;
        if (url.startsWith("jdbc:mariadb")) return Dialect.MARIADB;
        return Dialect.MYSQL;
    }

    /** Hands the pooled connections back, which a shared database account has a limited number of. */
    @Override
    public void close() {
        if (dataSource instanceof HikariDataSource pool) pool.close();
    }

    private record Settings(String url, String user, String password, int maxPool, int minIdle) {
    }

    /** A plugin folder without the properties file is the normal case, so only a malformed one is fatal. */
    private static Settings resolve(@NotNull Plugin plugin, @NotNull CollectableStorageConfig cfg) {
        DatabaseProperties external;
        try {
            external = DatabaseProperties.read(plugin.getDataFolder());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + DatabaseProperties.FILE_NAME, e);
        }

        if (external == null) {
            return new Settings(cfg.jdbcUrl(), cfg.username(), cfg.password(), cfg.maxPool(), cfg.minIdle());
        }

        plugin.getLogger().info("Read connection details from " + DatabaseProperties.FILE_NAME);
        return new Settings(
            external.jdbcUrl(),
            external.user(),
            external.password(),
            external.maxPool(),
            external.minPool()
        );
    }

    // Generic, so the DAO type is captured and the schema calls resolve against one interface
    private <E extends CollectableDao> void createSchema(@NotNull Class<E> type) {
        jdbi.useExtension(type, dao -> {
            dao.createTable();
            dao.createIdxPlayer();
            dao.createIdxPlayerNs();
            dao.createCountsTable();
            dao.createIdxCountsPlayerNs();
        });
    }

}
