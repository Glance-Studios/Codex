package com.glance.codex.platform.paper.notebooks.config;

import com.glance.codex.platform.paper.CodexPlugin;
import com.glance.codex.platform.paper.config.model.BookConfig;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.utils.format.StringUtils;
import com.google.inject.Injector;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;

/**
 * Loader/registrar for Note Book configs discovered via the config engine
 * <p>
 * Derives a {@code namespace} from the file path segments after {@code notes/}
 * (joined with underscores), and an {@code id} from either the config field or
 * the filename stem. Registers books into {@link NotebookRegistry}
 *
 * @author Cammy
 */
@Slf4j
@UtilityClass
public class NoteBookConfigLoader {

    private final String NOTES = "notes";

    /**
     * Processes a list of {@link NoteBookConfig} instances and registers their books
     *
     * <ul>
     *   <li>If {@code books} (map) is present, its keys are used as IDs</li>
     *   <li>If {@code book} (single) is present, ID is config {@code id} or filename stem</li>
     *   <li>Namespace is config {@code namespace} or derived from the file path after {@code notes/}</li>
     * </ul>
     *
     * @param injector DI injector used to obtain {@link NotebookRegistry}
     * @param configs  resolved notebook configs (each with an associated {@code filePath})
     */
    public void handleNoteBooks(
        @NotNull Injector injector,
        @NotNull List<NoteBookConfig> configs
    ) {
        final NotebookRegistry registry = injector.getInstance(NotebookRegistry.class);
        if (registry == null) return;

        for (NoteBookConfig cfg : configs) {
            Path path = cfg.filePath();

            // Resolve namespace
            String namespace = !StringUtils.isNullOrBlank(cfg.namespace())
                    ? cfg.namespace()
                    : deriveNamespaceFromPath(path);
            if (namespace.isBlank()) namespace = NOTES;

            Map<String, BookConfig> toRegister = new LinkedHashMap<>();

            // Multi-book form
            if (cfg.books() != null && !cfg.books().isEmpty()) {
                cfg.books().forEach((rawId, book) -> {
                    if (book == null) return;
                    String id = sanitizeId(rawId);
                    if (!id.isBlank()) {
                        toRegister.put(id, book);
                    } else {
                        log.debug("Skipped book with blank id in {}", path);
                    }
                });
            }

            // Single-book form
            if (cfg.book() != null) {
                String id = !StringUtils.isNullOrBlank(cfg.id())
                        ? sanitizeId(cfg.id())
                        : deriveIdFromPath(path);

                if (!id.isBlank()) {
                    toRegister.put(id, cfg.book());
                } else {
                    log.debug("Skipping single book with blank id in {}", path);
                }
            }

            if (toRegister.isEmpty()) {
                log.debug("{} had no books to register", path);
                continue;
            }

            for (var entry : toRegister.entrySet()) {
                registry.register(namespace, entry.getKey(), entry.getValue());
                CodexPlugin.getInstance().getLogger().info(
                    "Registered Note: " + namespace + ":" + entry.getKey() + " from " + path.getFileName()
                );
            }
        }
    }

    /**
     * Derive a namespace from the path segments after {@code notes/}
     * <p>
     * Example: {@code collectables/notes/mysterious/epic/bing.yml -> mysterious_epic}
     *
     * @param filePath full path to the config file
     * @return derived namespace, or {@code notes} if none can be derived
     */
    private String deriveNamespaceFromPath(Path filePath) {
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

    /**
     * Derive the book ID from the filename stem
     * <p>
     * Example: {@code bing.yml -> bing}
     *
     * @param filePath full path to the config file
     * @return sanitized filename stem (lowercase, {@code [a-z0-9_]}), or empty string
     */
    private String deriveIdFromPath(Path filePath) {
        if (filePath == null) return "";
        String name = filePath.getFileName() != null ? filePath.getFileName().toString() : "";
        int dot = name.lastIndexOf('.');
        String stem = (dot > 0) ? name.substring(0, dot) : name;
        return sanitizeId(stem);
    }

    /**
     * Find the index of a path segment equal to {@code target}
     *
     * @param path path to scan
     * @param target name to match (case-sensitive)
     * @return index of the segment, or {@code -1} if not found
     */
    private int indexPartOf(Path path, String target) {
        return indexPartOf(path, target, 0);
    }

    /**
     * Find the index of a path segment equal to {@code target}, starting at {@code start}
     *
     * @param path path to scan
     * @param target name to match (case-sensitive)
     * @param start starting index (inclusive)
     * @return index of the segment, or {@code -1} if not found
     */
    private int indexPartOf(Path path, String target, int start) {
        for (int i = start; i < path.getNameCount(); i++) {
            if (path.getName(i).toString().equals(target)) return i;
        }
        return -1;
    }

    /**
     * Sanitize an ID or path segment into {@code [a-z0-9_]}:
     * lowercase, non-alphanumerics to underscores, collapse/trim underscores
     *
     * @param s input string
     * @return sanitized identifier (possibly empty if nothing valid)
     */
    private String sanitizeId(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
    }

}
