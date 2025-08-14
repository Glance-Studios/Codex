package com.glance.codex.platform.paper.persistence.config;

import com.glance.codex.platform.paper.config.engine.annotation.Config;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigPath;
import com.google.auto.service.AutoService;
import com.google.inject.Singleton;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@Config(section = "storage")
@AutoService(Config.Handler.class)
@ToString
@Singleton
public class CollectableStorageConfig implements Config.Handler {

    @ConfigPath("backend") private Backend backend = Backend.FLATFILE;

    @ConfigPath(value = "flatfile.dir", comments = "Used when backend = 'FLATFILE'")
    private String flatFileDir = "playerdata/collectables";

    @ConfigPath("sql.jdbcUrl") private String jdbcUrl = "jdbc:sqlite:${plugin.data}/collectables.db";
    @ConfigPath("sql.username") private String username = "";
    @ConfigPath("sql.password") private String password = "";
    @ConfigPath("sql.pool.maxSize") private int maxPool = 6;
    @ConfigPath("sql.pool.minIdle") private int minIdle = 2;

    public enum Backend { FLATFILE, SQLITE, MYSQL }

}
