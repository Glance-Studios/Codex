package com.glance.codex.platform.paper.persistence.file;

import com.glance.codex.platform.paper.persistence.config.SqlBootstrap;
import com.glance.codex.platform.paper.persistence.sql.TestDatabases;

/** Runs the import contract against a real MariaDB, skipped when none is named. */
class FlatFileImportMariaDbTest extends FlatFileImportContract {

    @Override
    protected SqlBootstrap connect() {
        return TestDatabases.mariaDb();
    }
}
