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

    /** Failsafe cap used when recursive depth is not specified ({@code recursiveDepth < 0}) */
    private final int SYSTEM_MAX_DEPTH = 10;

    /**
     * Discover files under {@code baseDir} that match a given glob-like path pattern
     *
     * <p>When the {@code pattern} contains globs, it is split into a fixed prefix (walk base)
     * and a globbed tail. The filesystem walk starts at the walk base with a depth computed from
     * the tail, and the tail is matched against the path relative to the walk base</p>
     *
     * <p>When the {@code pattern} contains no globs, it is treated as a direct file path relative
     * to {@code baseDir}, and the method tries appending each extension from {@code exts}. If none
     * match and {@code eagerExtensionName} is true, it also tries the raw path as is (useful when
     * the pattern already includes the extension)</p>
     *
     * @param baseDir the root directory to search under (typically the plugin data folder)
     * @param pattern a 'forward slash delimited' path/glob relative to {@code baseDir}
     * @param exts allowed file extensions (no dot), e.g., {@code ["yml","yaml"]}
     * @param recursiveDepth effective depth when the tail contains {@code **};
     * if negative, {@link #SYSTEM_MAX_DEPTH} is used
     * @param eagerExtensionName if true, direct file lookups (no globs) will also try the raw name
     * as given, in addition to {@code name + "." + ext}
     * @return a list of matching regular files (unsorted; traversal order is filesystem dependent)
     * @throws RuntimeException if an {@link IOException} occurs while walking the file tree
     */
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
     * Compute the max recursive depth to check for this tail
     * <p>
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

    /**
     * Split a normalized pattern into a fixed, glob-free prefix (the {@code walkBase}) and a globbed tail
     *
     * <p>The {@code walkBase} is resolved from {@code base} by appending segments until the first segment
     * that contains a glob meta ({@code *}, {@code ?}, or {@code [}). The remainder (from that segment onward)
     * becomes the {@code tail}. If the pattern ends without a glob (defensive case), the tail defaults to
     * {@code "*"} so discovery matches immediate children</p>
     *
     * @param base the starting base directory (already normalized)
     * @param normalized the forward-slash-normalized pattern (may contain globs)
     * @return a {@link Split} record containing the {@code walkBase} and {@code tail}
     */
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

    /**
     * Attempt to resolve a direct file path (no globs) by trying each provided extension
     *
     * <p>Given {@code name="collectables/notes"}, and {@code exts=["yml","yaml"]}, this will try
     * {@code collectables/notes.yml} and {@code collectables/notes.yaml}. If none exist and
     * {@code eager} is true, it also tries {@code collectables/notes} as-is (useful when the
     * provided name already includes an extension)</p>
     *
     * @param base the base directory to resolve from
     * @param name a forward-slash path relative to {@code base}, with no globs
     * @param exts allowed extensions (no dot)
     * @param eager whether to also try the raw name as given
     * @return a singleton list containing the first matching file, or an empty list if none match
     */
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

    /**
     * Check whether {@code name} ends with one of the provided extensions (case-insensitive)
     *
     * @param name the filename to test
     * @param exts extensions without the leading dot (e.g., {@code "yml"})
     * @return {@code true} if {@code name} ends with any of the extensions; {@code false} otherwise
     */
    private boolean hasExt(String name, List<String> exts) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String ext : exts) {
            if (lower.endsWith("." + ext.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    /**
     * Convert a relative {@link Path} to a forward-slash string for glob matching
     * <p>
     * This keeps glob evaluation consistent across platforms
     *
     * @param rel the relative path
     * @return the Unix style string with {@code '/'} separators
     */
    private String toUnix(Path rel) {
        return rel.toString().replace('\\', '/');
    }

    /**
     * A split view of a pattern into a fixed walk base and a globbed tail
     *
     * @param walkBase absolute/normalized base directory that contains no globs
     * @param tail the remaining globbed pattern, to be matched relative to {@code walkBase}
     */
    private record Split(Path walkBase, String tail) {}

}
