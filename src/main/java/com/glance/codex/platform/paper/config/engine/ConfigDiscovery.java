package com.glance.codex.platform.paper.config.engine;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Glob-ish file discovery for @Config(fileName)
 * <p>
 * Rules:
 * - "something/*" => depth = 1
 * - "something/**" => depth = annotation recursiveDepth (-1 = unlimited)
 * - no asterisk => direct file (try with/without extensions)
 * <p>
 * NOTE: extension filtering is handled via 'exts' (strings without the dot, e.g. "yml")
 *
 * @author Cammy
 */
@Slf4j
@UtilityClass
public class ConfigDiscovery {

    private final int SYSTEM_MAX_DEPTH = 10;

    List<File> discover(
        @NonNull File baseDir,
        @NonNull String pattern,
        @NonNull List<String> exts,
        int recursiveDepth,
        boolean eagerExtensionName
    ) {
        final Path base = baseDir.toPath().normalize();
        final String normalized = pattern.replace('\\', '/');

        if (!containsGlob(normalized)) {
            return tryDirectFiles(base, normalized, exts, eagerExtensionName);
        }

        final Split split = splitFixedBase(base, normalized);
        final Path walkBase = split.walkBase;
        final String tail = split.tail;

        final int maxDepth = computeDepth(tail, recursiveDepth);

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + tail);

        try (Stream<Path> walk = Files.walk(walkBase, maxDepth)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> {
                       String relUnix = toUnix(walkBase.relativize(p));
                       return matcher.matches(Paths.get(relUnix)) && hasExt(p.getFileName().toString(), exts);
                    })
                    .map(Path::toFile)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed discovering files under" + walkBase + " for pattern" + pattern, e);
        }
    }

    /**
     * "*": shallow search (immediate children) => depth 1
     * "**": recursive, depth from annotation
     */
    private int computeDepth(String tail, int recursiveDepth) {
        if (tail.contains("**")) {
            return (recursiveDepth < 0) ? SYSTEM_MAX_DEPTH : recursiveDepth;
        }

        int segments = 0;
        for (String part : tail.split("/")) {
            if (!part.isEmpty()) segments++;
        }

        return Math.max(1, Math.min(SYSTEM_MAX_DEPTH, segments));
    }

    private Split splitFixedBase(Path base, String normalized) {
        String[] parts = normalized.split("/");
        int i = 0;
        for (; i < parts.length; i++) {
            String seg = parts[i];
            if (seg.isEmpty()) continue;
            if (seg.indexOf('*') >= 0 || seg.indexOf('?') >= 0 || seg.indexOf('[') >= 0) break;
        }

        Path walkBase = base;
        for (int j = 0; j < i; j++) {
            if (!parts[j].isEmpty()) {
                walkBase = walkBase.resolve(parts[j]);
            }
        }

        String tail;
        if (i >= parts.length) {
            // Pattern ended with a fixed path, treat as matching immediate children
            tail = "*";
        } else {
            tail = String.join("/", Arrays.copyOfRange(parts, i, parts.length));
        }

        return new Split(walkBase.normalize(), tail);
    }

    private List<File> tryDirectFiles(
            Path base,
            String name,
            List<String> exts,
            boolean eager
    ) {
        for (String ext : exts) {
            File f = base.resolve(name + "." + ext).toFile();
            if (f.isFile()) return List.of(f);
        }

        if (!eager) return List.of();

        // try as is (e.g. name already contains ext)
        File f = base.resolve(name).toFile();
        return f.isFile() ? List.of(f) : List.of();
    }

    /** True if the pattern contains any glob meta ({@code *, ?, [}). */
    private boolean containsGlob(String pattern) {
        return pattern.indexOf('*') >= 0 || pattern.indexOf('?') >= 0 || pattern.indexOf('[') >= 0;
    }

    private boolean hasExt(String name, List<String> exts) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String ext : exts) {
            if (lower.endsWith("." + ext.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private String toUnix(Path rel) {
        return rel.toString().replace('\\', '/');
    }

    private record Split(Path walkBase, String tail) {}

}
