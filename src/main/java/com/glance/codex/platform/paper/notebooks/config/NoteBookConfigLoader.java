package com.glance.codex.platform.paper.notebooks.config;

import com.glance.codex.platform.paper.config.model.BookConfig;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.utils.format.StringUtils;
import com.google.inject.Injector;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;

@Slf4j
@UtilityClass
public class NoteBookConfigLoader {

    private final String COLLECTABLES = "collectables";
    private final String NOTES = "notes";

    public void handleNoteBooks(
        @NotNull Injector injector,
        @NotNull List<NoteBookConfig> configs
    ) {
        final NotebookRegistry registry = injector.getInstance(NotebookRegistry.class);
        if (registry == null) return;

        log.info("Handling notebook configs {}", configs);

        for (NoteBookConfig cfg : configs) {
            Path path = cfg.filePath();
            log.info("About to try handle cfg {} at path {}", cfg, path);

            // Resolve namespace
            String namespace = !StringUtils.isNullOrBlank(cfg.namespace())
                    ? cfg.namespace()
                    : deriveNamespaceFromPath(path);
            if (namespace.isBlank()) namespace = NOTES;

            log.info("Handling notebook from {} derived namespace: {}", path, namespace);

            Map<String, BookConfig> toRegister = new LinkedHashMap<>();

            if (cfg.books() != null && !cfg.books().isEmpty()) {
                cfg.books().forEach((rawId, book) -> {
                    if (book == null) return;
                    String id = sanitizeId(rawId);
                    if (!id.isBlank()) {
                        toRegister.put(id, book);
                    } else {
                        log.warn("Skipped book with blank id in {}", path);
                    }
                });
            }

            if (cfg.book() != null) {
                String id = !StringUtils.isNullOrBlank(cfg.id())
                        ? sanitizeId(cfg.id())
                        : deriveIdFromPath(path);

                if (!id.isBlank()) {
                    toRegister.put(id, cfg.book());
                } else {
                    log.warn("Skipping single book with blank id in {}", path);
                }
            }

            if (toRegister.isEmpty()) {
                log.info("{} had no books to register", path);
                continue;
            }

            for (var entry : toRegister.entrySet()) {
                registry.register(namespace, entry.getKey(), entry.getValue());
                log.info("Registered note: {}:{} from {}", namespace, entry.getKey(), path.getFileName());
            }
        }
    }

    private String deriveNamespaceFromPath(Path filePath) {
        log.info("Attempting namespace derivation from {}", filePath);
        if (filePath == null) return NOTES;

        int notesIdx = indexPartOf(filePath, NOTES);
        if (notesIdx < 0) return "";

        int fileIdx = filePath.getNameCount() - 1;
        if (notesIdx >= fileIdx) return NOTES;

        List<String> parts = new ArrayList<>();
        for (int i = notesIdx + 1; i < fileIdx; i++) {
            parts.add(sanitizeId(filePath.getName(i).toString()));
        }

        String joined = String.join("_", parts);
        return joined.isBlank() ? NOTES : joined;
    }

    private String deriveIdFromPath(Path filePath) {
        if (filePath == null) return "";
        String name = filePath.getFileName() != null ? filePath.getFileName().toString() : "";
        int dot = name.lastIndexOf('.');
        String stem = (dot > 0) ? name.substring(0, dot) : name;
        return sanitizeId(stem);
    }

    private int indexPartOf(Path path, String target) {
        return indexPartOf(path, target, 0);
    }

    private int indexPartOf(Path path, String target, int start) {
        for (int i = start; i < path.getNameCount(); i++) {
            if (path.getName(i).toString().equals(target)) return i;
        }
        return -1;
    }

    private String sanitizeId(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
    }

}
