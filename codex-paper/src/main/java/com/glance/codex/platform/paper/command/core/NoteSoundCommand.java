package com.glance.codex.platform.paper.command.core;

import com.glance.codex.platform.paper.command.engine.CommandHandler;
import com.glance.codex.platform.paper.command.engine.suggestion.SuggestionHelpers;
import com.glance.codex.platform.paper.config.model.BookConfig;
import com.glance.codex.platform.paper.config.model.SoundEntry;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.platform.paper.notebooks.book.SoundCuePlayer;
import com.glance.codex.platform.paper.notebooks.edit.NoteBookEditService;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Authoring commands for a book's open sound cue
 * <p>
 * Edits go through {@link NoteBookEditService}, so every change is a surgical write to the
 * book's own entry followed by a reload, and is audible immediately
 *
 * @author Cammy
 */
@Slf4j
@Singleton
@AutoService(CommandHandler.class)
public class NoteSoundCommand implements CommandHandler {

    private static final int MAX_SUGGESTIONS = 60;

    private final NotebookRegistry notes;
    private final NoteBookEditService editService;
    private final SoundCuePlayer soundPlayer;

    @Inject
    public NoteSoundCommand(
            @NotNull final NotebookRegistry notes,
            @NotNull final NoteBookEditService editService,
            @NotNull final SoundCuePlayer soundPlayer
    ) {
        this.notes = notes;
        this.editService = editService;
        this.soundPlayer = soundPlayer;
    }

    /*
     * Declared locally rather than reusing NotesCommand's providers: cloud registers them
     * per handler class as it parses, so cross-class names depend on registration order.
     */

    @Suggestions("note-sound-namespaces")
    public List<String> suggestNamespaces(
            final CommandContext<CommandSender> ctx,
            final String input
    ) {
        return SuggestionHelpers.noteNamespaces(notes, input);
    }

    @Suggestions("note-sound-ids")
    public List<String> suggestIds(
            final CommandContext<CommandSender> ctx,
            final String input
    ) {
        return SuggestionHelpers.noteIds(notes, ctx.getOrDefault("namespace", "notes"), input);
    }

    /**
     * Vanilla sound keys, matched on either the full key or the part after the namespace
     * so typing "book" finds "minecraft:item.book.page_turn"
     * <p>
     * Sounds that only exist in a resource pack are unknown to the server, so the argument
     * still accepts anything that parses as a key
     */
    @Suggestions("sound-keys")
    public List<String> suggestSounds(
            final CommandContext<CommandSender> ctx,
            final String input
    ) {
        final String needle = (input == null ? "" : input).toLowerCase(Locale.ROOT);

        List<String> out = new ArrayList<>();
        for (org.bukkit.Sound sound : Registry.SOUNDS) {
            NamespacedKey namespaced = Registry.SOUNDS.getKey(sound);
            if (namespaced == null) continue;

            String key = namespaced.asString();
            if (needle.isEmpty()
                    || key.toLowerCase(Locale.ROOT).contains(needle)
                    || namespaced.getKey().toLowerCase(Locale.ROOT).startsWith(needle)) {
                out.add(key);
                if (out.size() >= MAX_SUGGESTIONS) break;
            }
        }
        return out;
    }

    @Command("collectables|journal notes sound list <namespace> <id>")
    @Permission("collectables.admin")
    public void list(
            @NotNull CommandSender sender,
            @Argument(value = "namespace", suggestions = "note-sound-namespaces") String namespace,
            @Argument(value = "id", suggestions = "note-sound-ids") String id
    ) {
        BookConfig cfg = resolve(sender, namespace, id);
        if (cfg == null) return;

        List<SoundEntry> layers = editService.readEffectiveLayers(
                new NamespacedKey(namespace, id), cfg.openSoundLayers());
        if (layers.isEmpty()) {
            sender.sendMessage("No open sound for " + namespace + ":" + id);
            return;
        }

        sender.sendMessage("Open cue for " + namespace + ":" + id + " (" + layers.size() + " layer(s)):");
        for (int i = 0; i < layers.size(); i++) {
            SoundEntry layer = layers.get(i);
            sender.sendMessage("  [" + i + "] " + layer.sound()
                    + "  vol=" + layer.volume()
                    + " pitch=" + layer.pitch()
                    + " delay=" + layer.delayTicks()
                    + (layer.enabled() ? "" : " (disabled)"));
        }
    }

    @Command("collectables|journal notes sound add <namespace> <id> <sound> [volume] [pitch] [delay]")
    @Permission("collectables.admin")
    public void add(
            @NotNull CommandSender sender,
            @Argument(value = "namespace", suggestions = "note-sound-namespaces") String namespace,
            @Argument(value = "id", suggestions = "note-sound-ids") String id,
            @Argument(value = "sound", suggestions = "sound-keys") String sound,
            @Argument("volume") @Default("1.0") float volume,
            @Argument("pitch") @Default("1.0") float pitch,
            @Argument("delay") @Default("0") long delay
    ) {
        BookConfig cfg = resolve(sender, namespace, id);
        if (cfg == null) return;

        SoundEntry layer = SoundEntry.of(sound).volume(volume).pitch(pitch).delayTicks(delay);
        if (layer.toAdventure() == null) {
            sender.sendMessage("'" + sound + "' is not a usable sound key");
            return;
        }

        // "add" starts from the cue currently heard, inherited or not, so a book that was
        // following the file default gains a layer rather than losing the rest.
        NoteBookEditService.SoundMutation result =
                mutate(sender, namespace, id, cfg, layers -> {
                    layers.add(layer);
                    return layers;
                });
        if (result == null) return;

        sender.sendMessage("Added " + sound + " to " + namespace + ":" + id
                + " (" + result.layers().size() + " layer(s))");
        if (!isKnownSound(sound)) {
            sender.sendMessage("Note: '" + sound + "' is not a sound this server knows about."
                    + " That is fine if it comes from your resource pack, otherwise check the spelling.");
        }
        if (result.wasInherited()) {
            sender.sendMessage("This book now has its own cue and no longer follows the file default.");
        }
        preview(sender, result.layers());
    }

    /** Whether the server has a sound registered under this key. Pack-only sounds will not. */
    private boolean isKnownSound(@NotNull String sound) {
        NamespacedKey key = NamespacedKey.fromString(sound);
        return key != null && Registry.SOUNDS.get(key) != null;
    }

    @Command("collectables|journal notes sound remove <namespace> <id> <index>")
    @Permission("collectables.admin")
    public void remove(
            @NotNull CommandSender sender,
            @Argument(value = "namespace", suggestions = "note-sound-namespaces") String namespace,
            @Argument(value = "id", suggestions = "note-sound-ids") String id,
            @Argument("index") int index
    ) {
        BookConfig cfg = resolve(sender, namespace, id);
        if (cfg == null) return;

        List<String> removed = new ArrayList<>();
        NoteBookEditService.SoundMutation result =
                mutate(sender, namespace, id, cfg, layers -> {
                    if (index < 0 || index >= layers.size()) return layers;
                    removed.add(layers.remove(index).sound());
                    return layers;
                });
        if (result == null) return;

        if (removed.isEmpty()) {
            sender.sendMessage("No layer [" + index + "]; use 'sound list' to see the indexes");
            return;
        }

        sender.sendMessage("Removed " + removed.get(0) + " from " + namespace + ":" + id
                + " (" + result.layers().size() + " layer(s) left)");
    }

    @Command("collectables|journal notes sound clear <namespace> <id>")
    @Permission("collectables.admin")
    public void clear(
            @NotNull CommandSender sender,
            @Argument(value = "namespace", suggestions = "note-sound-namespaces") String namespace,
            @Argument(value = "id", suggestions = "note-sound-ids") String id
    ) {
        if (resolve(sender, namespace, id) == null) return;
        if (!apply(sender, namespace, id, List.of())) return;

        sender.sendMessage("Cleared the cue on " + namespace + ":" + id
                + "; it now follows the notes file default again");
    }

    @Command("collectables|journal notes sound preview <namespace> <id>")
    @Permission("collectables.admin")
    public void previewCue(
            @NotNull Player sender,
            @Argument(value = "namespace", suggestions = "note-sound-namespaces") String namespace,
            @Argument(value = "id", suggestions = "note-sound-ids") String id
    ) {
        BookConfig cfg = resolve(sender, namespace, id);
        if (cfg == null) return;

        List<SoundEntry> layers = cfg.openSoundLayers();
        if (layers.isEmpty()) {
            sender.sendMessage("No open sound for " + namespace + ":" + id);
            return;
        }
        soundPlayer.play(layers, sender);
    }

    /** Look up a book, reporting to the sender if it is not there. */
    private @Nullable BookConfig resolve(
            @NotNull CommandSender sender,
            @NotNull String namespace,
            @NotNull String id
    ) {
        BookConfig cfg = notes.get(new NamespacedKey(namespace, id)).orElse(null);
        if (cfg == null) sender.sendMessage("No such book: " + namespace + ":" + id);
        return cfg;
    }

    /**
     * Apply a change to a book's cue and reload, reporting any failure to the sender
     * <p>
     * The current layers are read from disk inside the mutation, so consecutive commands
     * cannot overwrite each other while a reload is still in flight
     *
     * @return the result, or null if the write failed
     */
    private @Nullable NoteBookEditService.SoundMutation mutate(
            @NotNull CommandSender sender,
            @NotNull String namespace,
            @NotNull String id,
            @NotNull BookConfig cfg,
            @NotNull UnaryOperator<List<SoundEntry>> mutator
    ) {
        NamespacedKey key = new NamespacedKey(namespace, id);
        try {
            NoteBookEditService.SoundMutation result =
                    editService.mutateSoundLayers(key, cfg.openSoundLayers(), mutator);
            editService.reloadNotes();
            return result;
        } catch (Exception e) {
            sender.sendMessage("Could not save " + key.asString() + ": " + e.getMessage());
            log.error("Failed writing sound layers for {}", key.asString(), e);
            return null;
        }
    }

    /** Persist a cue verbatim and reload, reporting any failure to the sender. */
    private boolean apply(
            @NotNull CommandSender sender,
            @NotNull String namespace,
            @NotNull String id,
            @NotNull List<SoundEntry> layers
    ) {
        NamespacedKey key = new NamespacedKey(namespace, id);
        try {
            editService.writeSoundLayers(key, layers);
        } catch (Exception e) {
            sender.sendMessage("Could not save " + key.asString() + ": " + e.getMessage());
            log.error("Failed writing sound layers for {}", key.asString(), e);
            return false;
        }

        editService.reloadNotes();
        return true;
    }

    private void preview(@NotNull CommandSender sender, @NotNull List<SoundEntry> layers) {
        if (sender instanceof Player player) soundPlayer.play(layers, player);
    }

}
