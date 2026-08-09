package com.glance.codex.platform.paper.notebooks.edit;

import com.glance.codex.platform.paper.config.engine.codec.ConfigSerializable;
import com.glance.codex.platform.paper.config.engine.reload.ConfigReloader;
import com.glance.codex.platform.paper.config.model.SoundEntry;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.platform.paper.notebooks.config.NoteBookConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Reads and writes book text on behalf of the in game editor
 * <p>
 * Two responsibilities:
 * <ul>
 *     <li>Translating between the stored form, one string with {@code <newpage>} tokens,
 *     and the editor form, one string per physical book page</li>
 *     <li>Writing an edit back to the exact key it came from, without disturbing the rest
 *     of the file</li>
 * </ul>
 * The write is surgical rather than a re-serialization of the whole config bean: the file
 * is loaded, one path is set, and the file is saved. Every other entry keeps its on-disk
 * form, including comments, and defaults are not materialized into the file
 *
 * @author Cammy
 */
@Slf4j
@Singleton
public class NoteBookEditService {

    /** Mirrors the renderer's token so a round trip is symmetric. */
    private static final String PAGE_BREAK_TOKEN = "<newpage>";
    private static final Pattern PAGE_BREAK =
            Pattern.compile("(?i)" + Pattern.quote(PAGE_BREAK_TOKEN));

    private final NotebookRegistry registry;
    private final ConfigReloader reloader;

    @Inject
    public NoteBookEditService(
            @NotNull final NotebookRegistry registry,
            @NotNull final ConfigReloader reloader
    ) {
        this.registry = registry;
        this.reloader = reloader;
    }

    /**
     * Split stored content into physical editor pages
     * <p>
     * The token itself is consumed, so the author never sees it
     *
     * @param content stored content, may be null
     * @return one entry per page, never empty
     */
    public @NotNull List<String> toEditorPages(@Nullable String content) {
        if (content == null || content.isBlank()) return List.of("");

        List<String> pages = Arrays.stream(PAGE_BREAK.split(normalizeNewlines(content), -1))
                .map(this::trimEdgeNewlines)
                .toList();

        return pages.isEmpty() ? List.of("") : pages;
    }

    /**
     * Rejoin editor pages into stored content, reinstating the page break tokens
     *
     * @param pages page text exactly as typed
     * @return content suitable for the {@code content} config field
     */
    public @NotNull String fromEditorPages(@NotNull List<String> pages) {
        return pages.stream()
                .map(this::normalizeNewlines)
                .map(this::trimEdgeNewlines)
                .reduce((a, b) -> a + "\n" + PAGE_BREAK_TOKEN + "\n" + b)
                .orElse("");
    }

    /** True if a page contains the page break token as literal text, which would re-split on load. */
    public boolean containsLiteralToken(@NotNull List<String> pages) {
        return pages.stream().anyMatch(p -> PAGE_BREAK.matcher(p).find());
    }

    /**
     * Write edited text back to the book's own entry, then reload notebooks from disk
     * <p>
     * Also clears any explicit {@code pages} list, since the renderer prefers it over
     * {@code content} and the edit would otherwise appear to do nothing, and turns off
     * {@code collapseBlankLines} so blank lines the author typed survive rendering
     *
     * @param key the book being edited
     * @param content new content, already in stored form
     * @throws IOException if the file cannot be read or replaced
     */
    public void writeContent(@NotNull NamespacedKey key, @NotNull String content) throws IOException {
        BookSource source = registry.sourceOf(key).orElseThrow(() ->
                new IllegalStateException("No source file recorded for book " + key.asString()));

        File file = source.file().toFile();
        if (!file.isFile()) {
            throw new FileNotFoundException("Notes file is missing: " + file);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getConfigurationSection(source.yamlPath()) == null) {
            throw new IllegalStateException("Book section '" + source.yamlPath()
                    + "' no longer exists in " + file.getName());
        }

        yaml.set(source.pathOf("content"), content);
        yaml.set(source.pathOf("pages"), null);
        yaml.set(source.pathOf("collapseBlankLines"), false);

        writeAtomically(yaml, file);
        log.debug("Wrote content for {} to {}", key.asString(), file.getName());
    }

    /** Outcome of a cue mutation. */
    public record SoundMutation(@NotNull List<SoundEntry> layers, boolean wasInherited) {}

    /**
     * Read, change and write a book's cue as one step
     * <p>
     * The current layers come from the file rather than the loaded config, because a reload
     * is asynchronous: two commands in quick succession would otherwise both read the same
     * pre-edit state and the second would silently drop the first one's change
     * <p>
     * Synchronized so concurrent senders serialise rather than interleave
     *
     * @param key the book being edited
     * @param inherited layers the book would fall back to if it declares none of its own
     * @param mutator receives the current layers and returns the new ones
     * @return the layers written, and whether they started from the inherited cue
     * @throws IOException if the file cannot be read or replaced
     */
    public synchronized SoundMutation mutateSoundLayers(
            @NotNull NamespacedKey key,
            @NotNull List<SoundEntry> inherited,
            @NotNull UnaryOperator<List<SoundEntry>> mutator
    ) throws IOException {
        BookSource source = requireSource(key);
        File file = requireFile(source);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        requireSection(yaml, source, file);

        List<SoundEntry> own = readOwnLayers(yaml, source);
        boolean wasInherited = own.isEmpty();

        List<SoundEntry> updated = mutator.apply(
                new ArrayList<>(wasInherited ? inherited : own));

        applySoundLayers(yaml, source, updated);
        writeAtomically(yaml, file);

        log.debug("Wrote {} sound layer(s) for {} to {}", updated.size(), key.asString(), file.getName());
        return new SoundMutation(updated, wasInherited);
    }

    /**
     * The cue a book will actually play, read from disk rather than the loaded config
     * <p>
     * Reloads are asynchronous, so reading the in-memory config right after an edit can
     * still show the previous state. Going to the file keeps reporting honest
     *
     * @param key the book to inspect
     * @param inherited layers to fall back to when the book declares none of its own
     * @return the effective cue
     */
    public synchronized @NotNull List<SoundEntry> readEffectiveLayers(
            @NotNull NamespacedKey key,
            @NotNull List<SoundEntry> inherited
    ) {
        try {
            BookSource source = requireSource(key);
            File file = requireFile(source);

            List<SoundEntry> own = readOwnLayers(YamlConfiguration.loadConfiguration(file), source);
            return own.isEmpty() ? inherited : own;
        } catch (Exception e) {
            log.warn("Could not read cue for {} from disk, falling back to loaded config", key.asString(), e);
            return inherited;
        }
    }

    /** Layers this book declares itself, in either config form, as currently on disk. */
    private @NotNull List<SoundEntry> readOwnLayers(
            @NotNull YamlConfiguration yaml,
            @NotNull BookSource source
    ) {
        List<SoundEntry> layers = new ArrayList<>();

        List<?> raw = yaml.getList(source.pathOf("openSounds"));
        if (raw != null) {
            for (Object element : raw) {
                SoundEntry entry = decodeSound(element);
                if (entry != null) layers.add(entry);
            }
            if (!layers.isEmpty()) return layers;
        }

        SoundEntry single = decodeSound(yaml.get(source.pathOf("openSound")));
        if (single != null) layers.add(single);
        return layers;
    }

    private @Nullable SoundEntry decodeSound(@Nullable Object raw) {
        if (raw == null) return null;
        try {
            return ConfigSerializable.deserialize(raw, SoundEntry.class);
        } catch (Exception e) {
            log.warn("Skipping an unreadable sound layer: {}", raw, e);
            return null;
        }
    }

    private void applySoundLayers(
            @NotNull YamlConfiguration yaml,
            @NotNull BookSource source,
            @NotNull List<SoundEntry> layers
    ) {
        yaml.set(source.pathOf("openSound"), null);
        yaml.set(source.pathOf("openSounds"), layers.isEmpty()
                ? null
                : layers.stream().map(SoundEntry::serialize).toList());
    }

    /**
     * Write a book's open sound cue back to its own entry
     * <p>
     * Always writes the plural {@code openSounds} form and clears the singular one, so the
     * two cannot disagree. An empty list clears both, which drops the book back to whatever
     * the notes file's default provides
     *
     * @param key the book being edited
     * @param layers the cue to store
     * @throws IOException if the file cannot be read or replaced
     */
    public synchronized void writeSoundLayers(
            @NotNull NamespacedKey key,
            @NotNull List<SoundEntry> layers
    ) throws IOException {
        BookSource source = requireSource(key);
        File file = requireFile(source);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        requireSection(yaml, source, file);

        applySoundLayers(yaml, source, layers);
        writeAtomically(yaml, file);

        log.debug("Wrote {} sound layer(s) for {} to {}", layers.size(), key.asString(), file.getName());
    }

    private @NotNull BookSource requireSource(@NotNull NamespacedKey key) {
        return registry.sourceOf(key).orElseThrow(() ->
                new IllegalStateException("No source file recorded for book " + key.asString()));
    }

    private @NotNull File requireFile(@NotNull BookSource source) throws FileNotFoundException {
        File file = source.file().toFile();
        if (!file.isFile()) throw new FileNotFoundException("Notes file is missing: " + file);
        return file;
    }

    private void requireSection(
            @NotNull YamlConfiguration yaml,
            @NotNull BookSource source,
            @NotNull File file
    ) {
        if (yaml.getConfigurationSection(source.yamlPath()) == null) {
            throw new IllegalStateException("Book section '" + source.yamlPath()
                    + "' no longer exists in " + file.getName());
        }
    }

    /** Reload notebook configs so the edit takes effect without a restart. */
    public CompletableFuture<?> reloadNotes() {
        return reloader.reloadAllOf(NoteBookConfig.class);
    }

    /**
     * Replace a config file without leaving it half written
     * <p>
     * The previous contents are kept alongside as {@code .bak}, the new contents are staged
     * in a temp file in the same directory, and only then moved over the original. A crash
     * mid-write therefore costs the temp file, never the config
     */
    private void writeAtomically(@NotNull YamlConfiguration yaml, @NotNull File target) throws IOException {
        Path path = target.toPath();
        Path backup = path.resolveSibling(path.getFileName() + ".bak");

        Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);

        Path tmp = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            yaml.save(tmp.toFile());
            try {
                Files.move(tmp, path,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private @NotNull String normalizeNewlines(@NotNull String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }

    /** Drop newlines introduced by the token sitting on its own line, keep interior blanks. */
    private @NotNull String trimEdgeNewlines(@NotNull String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '\n') start++;
        while (end > start && s.charAt(end - 1) == '\n') end--;
        return s.substring(start, end);
    }

}
