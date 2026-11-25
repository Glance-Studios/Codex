package com.glance.codex.platform.paper.persistence.file;

import com.glance.codex.api.data.PlayerCollectables;
import com.glance.codex.api.data.storage.CollectableStorage;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Copies the per-player JSON files into whichever storage is configured now.
 *
 * A player already present in the target is left alone rather than merged, so an interrupted run
 * can simply be repeated and nothing that has since been played is overwritten.
 */
public final class FlatFileImport {

    /** Written once the whole directory has been read, so a boot does not walk it again. */
    public static final String MARKER_NAME = ".imported";

    private final Gson gson = new Gson();
    private final File directory;
    private final CollectableStorage target;
    private final Logger logger;

    public FlatFileImport(
        @NotNull File directory,
        @NotNull CollectableStorage target,
        @NotNull Logger logger
    ) {
        this.directory = directory;
        this.target = target;
        this.logger = logger;
    }

    public record Result(int imported, int skipped, List<String> failed) {

        public int total() {
            return imported + skipped + failed.size();
        }
    }

    /** @return true when there is player data to read and no marker saying it was already read */
    public boolean pending() {
        if (new File(directory, MARKER_NAME).isFile()) return false;
        File[] files = playerFiles();
        return files != null && files.length > 0;
    }

    /**
     * Reads every player file into the target, one player per transaction.
     *
     * Runs on the calling thread, since the futures it waits on are the storage layer's own.
     */
    public @NotNull Result run() {
        File[] files = playerFiles();
        if (files == null || files.length == 0) return new Result(0, 0, List.of());

        logger.info("Importing " + files.length + " collectables file(s) into SQL storage");

        int imported = 0;
        int skipped = 0;
        List<String> failed = new ArrayList<>();

        for (File file : files) {
            String name = file.getName();
            UUID playerId = parseId(name);
            if (playerId == null) {
                failed.add(name + " (not a player UUID)");
                continue;
            }

            try {
                if (hasData(playerId)) {
                    skipped++;
                    continue;
                }

                PlayerCollectables data = read(file);
                if (data == null) {
                    skipped++;
                    continue;
                }

                target.saveSnapshot(playerId, data).join();
                imported++;
            } catch (Exception e) {
                failed.add(name + " (" + e.getMessage() + ")");
            }
        }

        Result result = new Result(imported, skipped, failed);
        report(result);
        if (failed.isEmpty()) markDone();
        return result;
    }

    private void report(@NotNull Result result) {
        logger.info("Imported " + result.imported() + " player(s), skipped " + result.skipped()
                + " already present or empty, of " + result.total() + " file(s)");

        if (result.failed().isEmpty()) return;

        // No marker is written, so a fixed file is picked up by the next run
        logger.warning("Could not import " + result.failed().size()
                + " file(s), so the directory will be read again on the next start:");
        result.failed().forEach(entry -> logger.warning("  " + entry));
    }

    private boolean hasData(@NotNull UUID playerId) {
        PlayerCollectables existing = target.loadSnapshot(playerId).join();
        return !existing.unlocks().isEmpty() || !existing.counts().isEmpty();
    }

    private PlayerCollectables read(@NotNull File file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, PlayerCollectables.class);
        } catch (JsonParseException e) {
            throw new IOException("malformed JSON", e);
        }
    }

    private void markDone() {
        try {
            Files.writeString(new File(directory, MARKER_NAME).toPath(),
                    "Read into SQL storage. Delete this file to import these again.\n");
        } catch (IOException e) {
            logger.warning("Imported the collectables files but could not write "
                    + MARKER_NAME + ", so the next start will read them again: " + e.getMessage());
        }
    }

    private File[] playerFiles() {
        return directory.listFiles((dir, name) -> name.endsWith(".json"));
    }

    private static UUID parseId(@NotNull String fileName) {
        try {
            return UUID.fromString(fileName.substring(0, fileName.length() - ".json".length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
