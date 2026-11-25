package com.glance.codex.platform.paper.persistence.file;

import com.glance.codex.api.data.PlayerCollectables;
import com.glance.codex.platform.paper.persistence.config.SqlBootstrap;
import com.glance.codex.platform.paper.persistence.sql.JdbiCollectableStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** Reads JSON player files into a real store, the way a backend switch does on boot. */
abstract class FlatFileImportContract {

    private static final UUID PLAYER = UUID.fromString("8f1d2b7e-0000-4000-8000-000000000001");
    private static final UUID OTHER = UUID.fromString("8f1d2b7e-0000-4000-8000-000000000002");

    private Path directory;
    private SqlBootstrap sql;
    private JdbiCollectableStorage storage;
    private FlatFileImport job;

    /** Opens a connection, creating the schema, for one test. */
    protected abstract SqlBootstrap connect();

    @BeforeEach
    void open(@TempDir Path dir) throws IOException {
        directory = Files.createDirectories(dir.resolve("playerdata/collectables"));

        sql = connect();
        // A shared database keeps its rows between tests, so each starts from an empty table
        sql.jdbi().useHandle(handle -> {
            handle.execute("DELETE FROM collectable_unlocks");
            handle.execute("DELETE FROM collectable_counts");
        });

        storage = new JdbiCollectableStorage(sql);
        job = new FlatFileImport(directory.toFile(), storage, Logger.getLogger("test"));
    }

    @AfterEach
    void close() {
        // Null when the connection was skipped, since an aborted setup still runs the teardown
        if (storage != null) storage.close();
    }

    @Test
    void unlocksAndCountsBothSurviveTheMove() throws IOException {
        write(PLAYER, """
            {
              "unlocks": { "bestiary": ["owl", "pixie"] },
              "firstUnlockedAt": { "bestiary": { "owl": 1000, "pixie": 2000 } },
              "lastReplayedAt": { "bestiary": { "owl": 5000 } },
              "counts": { "bestiary": { "owl": 7 } }
            }
            """);

        FlatFileImport.Result result = job.run();

        assertEquals(1, result.imported());
        assertEquals(List.of(), result.failed());

        PlayerCollectables moved = storage.loadSnapshot(PLAYER).join();
        assertEquals(Set.of("owl", "pixie"), moved.unlocks().get("bestiary"));
        assertEquals(1000L, (long) moved.firstUnlockedAt().get("bestiary").get("owl"));
        assertEquals(5000L, (long) moved.lastReplayedAt().get("bestiary").get("owl"));
        assertEquals(7L, moved.count("bestiary", "owl"));
    }

    @Test
    void aPlayerAlreadyInTheDatabaseIsLeftAlone() throws IOException {
        PlayerCollectables played = new PlayerCollectables();
        played.markUnlock("bestiary", "banshee", 9000L);
        storage.saveSnapshot(PLAYER, played).join();

        write(PLAYER, """
            { "unlocks": { "bestiary": ["owl"] } }
            """);

        FlatFileImport.Result result = job.run();

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertEquals(Set.of("banshee"), storage.loadSnapshot(PLAYER).join().unlocks().get("bestiary"));
    }

    @Test
    void aFinishedRunIsNotRepeated() throws IOException {
        write(PLAYER, """
            { "unlocks": { "bestiary": ["owl"] } }
            """);

        assertTrue(job.pending());
        job.run();

        assertFalse(job.pending(), "the directory would be read again");
        assertTrue(Files.exists(directory.resolve(FlatFileImport.MARKER_NAME)));
    }

    @Test
    void aFileThatCannotBeReadIsReportedAndTheRunStaysPending() throws IOException {
        write(PLAYER, "{ \"unlocks\": { \"bestiary\": [\"owl\"] } }");
        Files.writeString(directory.resolve("not-a-uuid.json"), "{}");

        FlatFileImport.Result result = job.run();

        assertEquals(1, result.imported(), "a bad file stopped the good one");
        assertEquals(1, result.failed().size());
        assertTrue(job.pending(), "a failure should leave the directory to be read again");
    }

    @Test
    void anEmptyDirectoryIsNothingToDo() {
        assertFalse(job.pending());
        assertEquals(0, job.run().total());
    }

    @Test
    void everyPlayerInTheDirectoryIsMoved() throws IOException {
        write(PLAYER, "{ \"counts\": { \"bestiary\": { \"owl\": 1 } } }");
        write(OTHER, "{ \"counts\": { \"bestiary\": { \"owl\": 2 } } }");

        assertEquals(2, job.run().imported());
        assertEquals(1L, storage.loadSnapshot(PLAYER).join().count("bestiary", "owl"));
        assertEquals(2L, storage.loadSnapshot(OTHER).join().count("bestiary", "owl"));
    }

    private void write(UUID playerId, String json) throws IOException {
        Files.writeString(directory.resolve(playerId + ".json"), json);
    }
}
