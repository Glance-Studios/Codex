package com.glance.codex.platform.paper.persistence.sql;

import com.glance.codex.platform.paper.persistence.config.SqlBootstrap;

/** Runs the DAO contract against a real MariaDB, skipped when none is named. */
class CollectableDaoMariaDbTest extends CollectableDaoContract {

    @Override
    protected SqlBootstrap connect() {
        return TestDatabases.mariaDb();
    }
}
