package com.glance.codex.platform.paper.persistence.sql;

import com.glance.codex.platform.paper.persistence.config.SqlBootstrap;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class CollectableDaoSqliteTest extends CollectableDaoContract {

    @TempDir
    Path dir;

    @Override
    protected SqlBootstrap connect() {
        return TestDatabases.sqlite(dir.resolve("collectables.db"));
    }
}
