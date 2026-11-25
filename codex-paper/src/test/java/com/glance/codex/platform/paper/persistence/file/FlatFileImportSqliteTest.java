package com.glance.codex.platform.paper.persistence.file;

import com.glance.codex.platform.paper.persistence.config.SqlBootstrap;
import com.glance.codex.platform.paper.persistence.sql.TestDatabases;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class FlatFileImportSqliteTest extends FlatFileImportContract {

    @TempDir
    Path dir;

    @Override
    protected SqlBootstrap connect() {
        return TestDatabases.sqlite(dir.resolve("collectables.db"));
    }
}
