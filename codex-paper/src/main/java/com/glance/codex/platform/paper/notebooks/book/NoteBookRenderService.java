package com.glance.codex.platform.paper.notebooks.book;

import com.glance.codex.api.text.PlaceholderService;
import com.glance.codex.platform.paper.config.model.BookConfig;
import com.glance.codex.platform.paper.notebooks.NotebookRegistry;
import com.glance.codex.platform.paper.text.PlaceholderUtils;
import com.glance.codex.utils.lifecycle.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Slf4j
@Singleton
@AutoService(Manager.class)
public class NoteBookRenderService implements Manager {

    private final MiniMessage MM = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /** Token authors can drop into content to force a page break. Case-insensitive. */
    private static final String PAGE_BREAK_TOKEN = "<newpage>";

    private final int MAX_BOOK_PAGES = 100;

    private final PlaceholderService placeholderService;

    @Inject
    public NoteBookRenderService(
            @NotNull PlaceholderService placeholderService
    ) {
        this.placeholderService = placeholderService;
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
        meta.author(render(authorRaw, cfg.useMiniMessage()));

        List<String> pageStrings = resolvePages(cfg, player, full);
        if (cfg.titlePage()) {
            // Item titles are capped at 32 chars; the title page can show the full name.
            String pageTitleSrc = safe(cfg.displayTitle()).isBlank() ? safe(cfg.title()) : cfg.displayTitle();
            String pageTitleRaw = placeholderService.apply(safe(pageTitleSrc), player, full);
            pageStrings.add(0, buildTitlePage(pageTitleRaw, authorRaw, cfg));
        }
        if (pageStrings.isEmpty()) pageStrings = new ArrayList<>(List.of(""));

        List<Component> comps = new ArrayList<>(pageStrings.size());
        for (String s : pageStrings) comps.add(render(s, cfg.useMiniMessage()));
        meta.pages(comps);

        book.setItemMeta(meta);
        return book;
    }

    /**
     * Effective page width in pixels. A non-positive value (e.g. the config omitted the key and
     * the primitive decoded to 0) falls back to the vanilla default rather than collapsing to 1px.
     */
    private int pageWidth(@NotNull BookConfig cfg) {
        return cfg.pageWidthPixels() > 0 ? cfg.pageWidthPixels() : BookFontMetrics.DEFAULT_PAGE_WIDTH;
    }

    /** Effective visible lines per page, falling back to the vanilla default when unset/invalid. */
    private int linesPerPage(@NotNull BookConfig cfg) {
        return cfg.maxLinesPerPage() > 0 ? cfg.maxLinesPerPage() : BookFontMetrics.DEFAULT_LINES_PER_PAGE;
    }

    public List<String> resolvePages(
            @NotNull BookConfig cfg,
            @Nullable Player player,
            @Nullable Map<String, String> placeholders
    ) {
        final int pageWidth = pageWidth(cfg);
        final int linesPerPage = linesPerPage(cfg);

        // Explicit pages: each entry is one authored page. Still safety-wrap each so a
        // too-wide/too-tall page can't clip, but keep the author's page boundaries.
        List<String> pagesExplicit = cfg.pages();
        if (pagesExplicit != null && !pagesExplicit.isEmpty()) {
            List<String> out = new ArrayList<>();
            for (String p : pagesExplicit) {
                String resolved = placeholderService.apply(safe(p), player, placeholders);
                List<String> lines = wrap(resolved, pageWidth, cfg);
                out.addAll(paginateBalanced(lines, linesPerPage));
            }
            return clampPages(out);
        }

        String content = placeholderService.apply(safe(cfg.content()), player, placeholders);

        // Author-controlled page breaks: split first, so each segment starts a fresh page.
        List<String> pages = new ArrayList<>();
        for (String segment : splitOnPageBreak(content)) {
            List<String> lines = wrap(segment, pageWidth, cfg);
            pages.addAll(paginateBalanced(lines, linesPerPage));
        }
        return clampPages(pages);
    }

    /**
     * Word-wraps text to fit within {@code maxPixelWidth} using vanilla font metrics.
     * Honors explicit newlines as hard breaks; over-long single words are broken by
     * character so they can never overflow.
     */
    private List<String> wrap(String content, int maxPixelWidth, @NotNull BookConfig cfg) {
        final boolean collapseBlank = cfg.collapseBlankLines();
        final boolean useMM = cfg.useMiniMessage();
        final String normalized = normalizeNewlines(content);

        List<String> out = new ArrayList<>();
        String[] paragraphs = normalized.split("\n", -1);

        for (String para : paragraphs) {
            if (para.isBlank()) {
                if (collapseBlank && (out.isEmpty() || out.getLast().isBlank())) continue;
                out.add("");
                continue;
            }

            StringBuilder line = new StringBuilder();
            int lineWidth = 0;
            for (String word : para.trim().split("\\s+")) {
                int wordWidth = measure(word, useMM);

                // Word alone is wider than a page: hard-break it by character.
                if (wordWidth > maxPixelWidth) {
                    if (lineWidth > 0) { out.add(line.toString()); line.setLength(0); lineWidth = 0; }
                    for (String piece : breakLongWord(word, maxPixelWidth, useMM)) {
                        out.add(piece);
                    }
                    // last piece may still have room to continue the line
                    String last = out.removeLast();
                    line.append(last);
                    lineWidth = measure(last, useMM);
                    continue;
                }

                int spaceWidth = (lineWidth == 0) ? 0 : BookFontMetrics.SPACE_ADVANCE;
                if (lineWidth + spaceWidth + wordWidth <= maxPixelWidth) {
                    if (lineWidth > 0) { line.append(' '); lineWidth += BookFontMetrics.SPACE_ADVANCE; }
                    line.append(word);
                    lineWidth += wordWidth;
                } else {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                    lineWidth = wordWidth;
                }
            }
            if (lineWidth > 0 || !line.isEmpty()) out.add(line.toString());
        }
        return out;
    }

    /** Breaks a single word that is wider than a page into page-width character chunks. */
    private List<String> breakLongWord(String word, int maxPixelWidth, boolean useMM) {
        List<String> pieces = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        int width = 0;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            int cw = BookFontMetrics.charWidth(c);
            if (width + cw > maxPixelWidth && chunk.length() > 0) {
                pieces.add(chunk.toString());
                chunk.setLength(0);
                width = 0;
            }
            chunk.append(c);
            width += cw;
        }
        if (chunk.length() > 0) pieces.add(chunk.toString());
        if (pieces.isEmpty()) pieces.add(word);
        return pieces;
    }

    /** A page whose final page holds this many visible lines or fewer is treated as an orphan. */
    private static final int ORPHAN_MAX_LINES = 3;

    /**
     * Paginates wrapped lines, then removes "orphan" pages — a stray word or two left alone on a
     * final page because a paragraph-break blank line nudged the content just over a page boundary.
     * <p>
     * Step 1 drops a paragraph-break blank line when doing so collapses a whole page away (pulling
     * the stray tail back onto the previous page). Step 2, for genuinely long content with no
     * removable blank, balances the last two pages so neither is left nearly empty.
     */
    private List<String> paginateBalanced(List<String> lines, int maxLinesPerPage) {
        final int max = Math.max(1, maxLinesPerPage);

        List<String> work = new ArrayList<>(lines);
        trimBlankEdges(work);
        if (work.isEmpty()) return new ArrayList<>();

        // Step 1: remove a paragraph-break blank only when it eliminates a whole (orphan) page.
        while (true) {
            List<List<String>> pgs = splitPages(work, max);
            if (pgs.size() <= 1 || nonBlank(pgs.get(pgs.size() - 1)) > ORPHAN_MAX_LINES) break;
            int bi = lastBlankIndex(work);
            if (bi < 0) break;
            List<String> trial = new ArrayList<>(work);
            trial.remove(bi);
            if (splitPages(trial, max).size() < pgs.size()) work = trial;
            else break;
        }

        List<List<String>> pages = splitPages(work, max);

        // Step 2: still an orphan (no removable blank) -> balance the final two pages evenly.
        if (pages.size() >= 2 && nonBlank(pages.get(pages.size() - 1)) <= ORPHAN_MAX_LINES) {
            List<String> combined = new ArrayList<>(pages.get(pages.size() - 2));
            combined.addAll(pages.get(pages.size() - 1));
            trimBlankEdges(combined);
            int half = (combined.size() + 1) / 2;
            List<String> a = new ArrayList<>(combined.subList(0, half));
            List<String> b = new ArrayList<>(combined.subList(half, combined.size()));
            pages.set(pages.size() - 2, a);
            pages.set(pages.size() - 1, b);
        }

        List<String> out = new ArrayList<>(pages.size());
        for (List<String> p : pages) {
            List<String> pp = new ArrayList<>(p);
            // trim a wasted paragraph gap left at the very top of a page
            while (!pp.isEmpty() && pp.get(0).isBlank()) pp.remove(0);
            out.add(String.join("\n", pp));
        }
        return out;
    }

    private List<List<String>> splitPages(List<String> lines, int max) {
        List<List<String>> pages = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        for (String l : lines) {
            cur.add(l);
            if (cur.size() >= max) { pages.add(cur); cur = new ArrayList<>(); }
        }
        if (!cur.isEmpty()) pages.add(cur);
        return pages;
    }

    private long nonBlank(List<String> page) {
        return page.stream().filter(s -> !s.isBlank()).count();
    }

    private int lastBlankIndex(List<String> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) if (lines.get(i).isBlank()) return i;
        return -1;
    }

    private void trimBlankEdges(List<String> lines) {
        while (!lines.isEmpty() && lines.get(0).isBlank()) lines.remove(0);
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) lines.remove(lines.size() - 1);
    }

    /** Builds a centered title page showing the book's title and author. */
    private String buildTitlePage(String title, String author, @NotNull BookConfig cfg) {
        final int pageWidth = pageWidth(cfg);
        final boolean useMM = cfg.useMiniMessage();

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n\n");
        for (String line : wrap(safe(title), pageWidth, cfg)) {
            sb.append(center(line, pageWidth, useMM)).append('\n');
        }
        sb.append('\n');
        if (!safe(author).isBlank()) {
            for (String line : wrap("by " + author, pageWidth, cfg)) {
                sb.append(center(line, pageWidth, useMM)).append('\n');
            }
        }
        return sb.toString();
    }

    /** Left-pads a line with spaces so it renders roughly centered on the page. */
    private String center(String line, int pageWidth, boolean useMM) {
        int textWidth = measure(line, useMM);
        int pad = (pageWidth - textWidth) / 2;
        if (pad <= 0) return line;
        int spaces = pad / BookFontMetrics.SPACE_ADVANCE;
        return " ".repeat(spaces) + line;
    }

    /**
     * Splits content on the page-break token, trimming surrounding blank lines so each
     * segment starts cleanly at the top of a new page.
     */
    private List<String> splitOnPageBreak(String content) {
        String normalized = normalizeNewlines(content);
        String[] parts = normalized.split("(?i)" + java.util.regex.Pattern.quote(PAGE_BREAK_TOKEN));
        List<String> segments = new ArrayList<>(parts.length);
        for (String part : parts) {
            segments.add(part.strip());
        }
        return segments;
    }

    /**
     * Measures the visible pixel width of raw text. When MiniMessage is enabled, tags
     * are stripped (they render to zero-width) so only visible glyphs are counted.
     */
    private int measure(String raw, boolean useMM) {
        String visible = useMM ? PLAIN.serialize(MM.deserialize(raw)) : raw;
        return BookFontMetrics.width(visible);
    }

    /**
     * Renders a raw string to a component. With MiniMessage enabled it is parsed for
     * tags; otherwise tags are escaped so the text renders literally.
     */
    private Component render(String raw, boolean useMM) {
        return useMM ? MM.deserialize(raw) : MM.deserialize(MM.escapeTags(raw));
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
