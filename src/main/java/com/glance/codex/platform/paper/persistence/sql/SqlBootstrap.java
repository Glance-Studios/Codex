package com.glance.codex.platform.paper.persistence.sql;

import com.glance.codex.platform.paper.persistence.CollectableStorageConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.plugin.Plugin;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;

@Singleton
@Getter
@Accessors(fluent = true)
public class SqlBootstrap {

    private final DataSource dataSource;
    private final Jdbi jdbi;
    private final Dialect dialect;

    public enum Dialect { SQLITE, MYSQL, MARIADB }

    @Inject
    public SqlBootstrap(
        @NotNull Plugin plugin,
        @NotNull CollectableStorageConfig cfg
    ) {
        String url = cfg.jdbcUrl();

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(url);

        if (!cfg.username().isEmpty()) hc.setUsername(cfg.username());
        if (!cfg.password().isEmpty()) hc.setPassword(cfg.password());

        hc.setMaximumPoolSize(cfg.maxPool());
        hc.setMinimumIdle(cfg.minIdle());

        this.dataSource = new HikariDataSource(hc);

        this.jdbi = Jdbi.create(this.dataSource).installPlugins();
        this.dialect = url.startsWith("jdbc:sqlite") ? Dialect.SQLITE : Dialect.MYSQL;

        // Create schema once
        if (dialect == Dialect.SQLITE) {
            jdbi.useExtension(SqliteCollectableDao.class, dao -> {
                dao.createTable();
                dao.createIdxPlayer();
                dao.createIdxPlayerNs();
            });
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
