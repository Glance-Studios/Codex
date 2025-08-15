package com.glance.codex.platform.paper.notebooks.book;

import com.glance.codex.platform.paper.api.text.PlaceholderService;
import com.glance.codex.platform.paper.config.model.BookConfig;
import com.glance.codex.platform.paper.config.model.LineWrapOptions;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.platform.paper.text.PlaceholderUtils;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Slf4j
@Singleton
@AutoService(Manager.class)
public class NoteBookRenderService implements Manager {

    private final MiniMessage MM = MiniMessage.miniMessage();
    private final PlaceholderService placeholderService;
    private final NotebookRegistry registry;
    private final Plugin plugin;

    private final int DEFAULT_WRAP_WIDTH = 24;
    private final int MAX_BOOK_PAGES = 100;

    @Inject
    public NoteBookRenderService(
            @NotNull Plugin plugin,
            @NotNull PlaceholderService placeholderService,
            @NotNull NotebookRegistry registry
            ) {
        this.plugin = plugin;
        this.placeholderService = placeholderService;
        this.registry = registry;
    }

    public ItemStack buildWrittenBook(
            @NotNull BookConfig cfg,
            @Nullable Player player,
            @Nullable Map<String, String> placeholders
    ) {
        final ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        final BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) throw new IllegalStateException("BookMeta was null from " + book);

        Map<String, String> full = (player != null)
                ? PlaceholderUtils.appendPlayerTags(player, placeholders)
                : (placeholders != null ? new HashMap<>(placeholders) : Collections.emptyMap());

        String titleRaw = placeholderService.apply(safe(cfg.title()), player, full);
        String authorRaw = placeholderService.apply(safe(cfg.author()), player, full);

        meta.setTitle(titleRaw);
        meta.author(MM.deserialize(authorRaw));

        List<String> pageStrings = resolvePages(cfg, player, full);
        if (pageStrings.isEmpty()) pageStrings = List.of("");

        List<Component> comps = new ArrayList<>(pageStrings.size());
        for (String s : pageStrings) comps.add(MM.deserialize(s));
        meta.pages(comps);

        book.setItemMeta(meta);
        return book;
    }

    public List<String> resolvePages(
            @NotNull BookConfig cfg,
            @Nullable Player player,
            @Nullable Map<String, String> placeholders
    ) {
        List<String> pagesExplicit = cfg.pages();
        if (pagesExplicit != null && !pagesExplicit.isEmpty()) {
            List<String> out = new ArrayList<>(pagesExplicit.size());
            for (String p : pagesExplicit) {
                out.add(placeholderService.apply(safe(p), player, placeholders));
            }
            return clampPages(out);
        }

        String content = placeholderService.apply(safe(cfg.content()), player, placeholders);
        List<String> lines = wrap(content, (cfg.wrap() != null)
                ? cfg.wrap()
                : new LineWrapOptions(DEFAULT_WRAP_WIDTH, true),
                cfg.collapseBlankLines());

        List<String> pages = paginate(lines, Math.max(1, cfg.maxLinesPerPage()));

        return clampPages(pages);
    }

    private List<String> wrap(String content, @NotNull LineWrapOptions opts, boolean collapseBlank) {
        final int width = Math.max(1, opts.maxLineLength());
        final String normalized = normalizeNewlines(content);

        List<String> out = new ArrayList<>();
        String[] paragraphs = normalized.split("\n", -1);

        for (String para : paragraphs) {
            if (collapseBlank && para.isBlank()) {
                // collapse consecutive blanks to a single blank line
                if (out.isEmpty() || out.getLast().isBlank()) continue;
                out.add("");
                continue;
            }
            if (para.isEmpty()) { out.add(""); continue; }

            // simple word wrap
            String[] words = para.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String w : words) {
                if (line.isEmpty()) {
                    line.append(w);
                } else if (line.length() + 1 + w.length() <= width) {
                    line.append(' ').append(w);
                } else {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(w);
                }
            }
            if (!line.isEmpty()) out.add(line.toString());
        }

        return out;
    }

    private List<String> paginate(List<String> lines, int maxLinesPerPage) {
        List<String> pages = new ArrayList<>();
        if (lines.isEmpty()) return pages;

        int max = Math.max(1, maxLinesPerPage);
        StringBuilder page = new StringBuilder();
        int count = 0;

        for (String l : lines) {
            if (count == 0) page.append(l);
            else page.append('\n').append(l);

            if (++count >= max) {
                pages.add(page.toString());
                page.setLength(0);
                count = 0;
            }
        }
        if (count > 0) pages.add(page.toString());
        return pages;
    }

    private String normalizeNewlines(String s) {
        return s.replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private List<String> clampPages(List<String> pages) {
        return (pages.size() <= MAX_BOOK_PAGES)
                ? pages
                : new ArrayList<>(pages.subList(0, MAX_BOOK_PAGES));
    }

    private String safe(String s) { return (s == null) ? "" : s; }

}
