# Codex
Minecraft Java Plugin for Discoverables and Collectables

## Storage

`storage.backend` in `config.yml` takes `FLATFILE`, `SQLITE`, `MYSQL` or `MARIADB`. Flat files are
one JSON per player and fine for data that changes a handful of times; anything that counts wants a
database, since a count rewrites the whole file on every change.

The dialect is read from the connection URL rather than from the backend name, so a `jdbc:mariadb://`
URL always speaks MariaDB. A missing driver stops the plugin loading instead of falling back to flat
files, because a silent fallback shows every player an empty journal and then collects new progress
somewhere else.

### Credentials

Put a `database.properties` next to `config.yml` and it wins over `storage.sql.*`, so a deployment
can ship settings while the credentials stay on the server:

```properties
db.host=localhost
db.port=3306
db.database=codex
db.user=codex
db.password=
db.pool.min=2
db.pool.max=8
```

`db.pool.min` and `db.pool.max` are optional. Every other key is required once the file exists.

### Moving off flat files

Switching the backend to SQL imports whatever is in `playerdata/collectables` on the next start, one
player per transaction. A player already present in the database is skipped rather than merged, so
an interrupted run can simply be repeated. A `.imported` marker is written once the whole directory
has been read without failures; delete it to run the import again.

`/collectables storage` reports the backend and whether anything is waiting, and
`/collectables storage import` reads the files again without regard for the marker. Both need
`collectables.admin`.

### Tests

The SQL layer is the one part testable off a server. `./gradlew test` covers SQLite; point it at a
MariaDB to run the same contract against the other dialect:

```
CODEX_TEST_MARIADB_URL=jdbc:mariadb://127.0.0.1:3307/codex_test
CODEX_TEST_MARIADB_USER=...
CODEX_TEST_MARIADB_PASSWORD=...
```

Without those the MariaDB cases are skipped rather than failed.
