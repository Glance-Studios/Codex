package com.glance.codex.platform.paper.notebooks.edit;

import com.glance.codex.platform.paper.config.model.BookConfig;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * In game editing of a note book's text
 * <p>
 * Minecraft has no API to force the writing screen open; the client only shows it when a
 * player uses a book and quill they are holding. So the flow is to hand the author a
 * writable book seeded with the current text, marked so the save can be routed back to the
 * right entry, and let them open it themselves
 * <p>
 * What the author types is stored verbatim. Page boundaries become {@code <newpage>} tokens
 * and nothing is parsed as formatting, so the rendered lore matches what was typed
 *
 * @author Cammy
 */
@Slf4j
@Singleton
@AutoService({Manager.class, Listener.class})
public class BookEditor implements Manager, Listener {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final Plugin plugin;
    private final NotebookRegistry registry;
    private final NoteBookEditService editService;

    /** Marks an item as an editor session for a specific book. */
    private final NamespacedKey markerKey;

    @Inject
    public BookEditor(
            @NotNull final Plugin plugin,
            @NotNull final NotebookRegistry registry,
            @NotNull final NoteBookEditService editService
    ) {
        this.plugin = plugin;
        this.registry = registry;
        this.editService = editService;
        this.markerKey = new NamespacedKey(plugin, "editing_note");
    }

    /**
     * Give a player a writable copy of a book's current text
     *
     * @param player the author
     * @param key the book to edit
     * @return a failure reason, or null if the editor book was handed over
     */
    public String give(@NotNull Player player, @NotNull NamespacedKey key) {
        BookConfig cfg = registry.get(key).orElse(null);
        if (cfg == null) return "No such book: " + key.asString();

        if (registry.sourceOf(key).isEmpty()) {
            return "Book " + key.asString() + " has no source file recorded, so it cannot be edited";
        }

        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();
        if (meta == null) return "Could not build the editor book";

        meta.pages(editService.toEditorPages(cfg.content()).stream()
                .map(page -> (Component) Component.text(page))
                .toList());
        meta.displayName(Component.text("Editing: " + key.asString()));
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, key.asString());
        item.setItemMeta(meta);

        if (!player.getInventory().addItem(item).isEmpty()) {
            return "No room in " + player.getName() + "'s inventory for the editor book";
        }

        return null;
    }

    @EventHandler
    public void onEditBook(PlayerEditBookEvent event) {
        BookMeta previous = event.getPreviousBookMeta();
        String rawKey = previous.getPersistentDataContainer().get(markerKey, PersistentDataType.STRING);
        if (rawKey == null) return;

        NamespacedKey key = NamespacedKey.fromString(rawKey);
        if (key == null) {
            log.warn("Editor book carried an unparseable key '{}'", rawKey);
            return;
        }

        Player player = event.getPlayer();
        List<String> pages = event.getNewBookMeta().pages().stream()
                .map(PLAIN::serialize)
                .toList();

        if (editService.containsLiteralToken(pages)) {
            player.sendMessage(Component.text(
                    "Heads up: your text contains '<newpage>', which will act as a page break."));
        }

        try {
            editService.writeContent(key, editService.fromEditorPages(pages));
        } catch (Exception e) {
            player.sendMessage(Component.text("Could not save " + key.asString() + ": " + e.getMessage()));
            log.error("Failed writing edited content for {}", key.asString(), e);
            return;
        }

        // The edit event fires before the item is updated, so clear it on the next tick.
        plugin.getServer().getScheduler().runTask(plugin, () -> clearEditorBooks(player));

        editService.reloadNotes().thenRun(() ->
                player.sendMessage(Component.text("Saved " + key.asString() + " (" + pages.size() + " page(s))")));
    }

    /** Remove any editor books from a player's inventory, signed or not. */
    private void clearEditorBooks(@NotNull Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null) continue;

            ItemMeta meta = stack.getItemMeta();
            if (meta == null) continue;

            if (meta.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

}
