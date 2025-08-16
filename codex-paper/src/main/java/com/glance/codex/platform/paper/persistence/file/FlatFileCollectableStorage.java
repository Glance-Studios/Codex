package com.glance.codex.platform.paper.persistence.file;

import com.glance.codex.api.data.PlayerCollectables;
import com.glance.codex.api.data.storage.CollectableStorage;
import com.glance.codex.platform.paper.persistence.config.CollectableStorageConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class FlatFileCollectableStorage implements CollectableStorage {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File baseDir;
    private final Object fileIOLock = new Object();

    @Inject
    public FlatFileCollectableStorage(
        @NotNull Plugin plugin,
        @NotNull CollectableStorageConfig config
    ) {
        if (config.backend() != CollectableStorageConfig.Backend.FLATFILE) {
            throw new IllegalStateException("Configured Storage Backend was '" + config.backend().name() +
                    "' when attempting to create FlatFileCollectableStorage, this is not allowed. " +
                    "Please use " + CollectableStorageConfig.Backend.FLATFILE.name());
        }
        this.baseDir = new File(plugin.getDataFolder(), config.flatFileDir());
        this.baseDir.mkdirs();
    }

    private File file(UUID playerId) {
        return new File(baseDir, playerId + ".json");
    }

    private PlayerCollectables loadData(@NotNull UUID playerId) {
        File f = file(playerId);
        if (!f.exists()) return new PlayerCollectables();
        try (Reader r = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            return gson.fromJson(r, PlayerCollectables.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveData(@NotNull UUID playerId, @NotNull PlayerCollectables data) {
        File f = file(playerId);
        try (Writer w = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(data, w);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Set<String>> loadUnlockedIds(@NotNull UUID playerId, @NotNull String namespace) {
        return CompletableFuture.supplyAsync(() -> loadData(playerId).unlocks().getOrDefault(namespace, Set.of()));
    }

    @Override
    public CompletableFuture<Boolean> putUnlock(@NotNull UUID playerId, @NotNull String namespace, @NotNull String id, long whenMillis) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerCollectables data = loadData(playerId);
            boolean added = data.markUnlock(namespace, id, whenMillis);
            if (added) saveData(playerId, data);
            return added;
        });
    }

    @Override
    public CompletableFuture<Void> recordReplay(@NotNull UUID playerId, @NotNull String namespace, @NotNull String id, long whenMillis) {
        return CompletableFuture.runAsync(() -> {
            PlayerCollectables data = loadData(playerId);
            data.markReplay(namespace, id, whenMillis);
            saveData(playerId, data);
        });
    }

    @Override
    public CompletableFuture<Boolean> isUnlocked(@NotNull UUID playerId, @NotNull String namespace, @NotNull String id) {
        return CompletableFuture.supplyAsync(() -> loadData(playerId).isUnlocked(namespace, id));
    }

    @Override
    public CompletableFuture<PlayerCollectables> loadSnapshot(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> loadData(playerId));
    }

    @Override
    public CompletableFuture<Void> saveSnapshot(@NotNull UUID playerId, @NotNull PlayerCollectables snapshot) {
        return CompletableFuture.runAsync(() -> saveData(playerId, snapshot));
    }

}
