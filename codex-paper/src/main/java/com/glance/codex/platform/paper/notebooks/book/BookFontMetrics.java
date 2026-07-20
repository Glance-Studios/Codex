package com.glance.codex.platform.paper.notebooks.book;

/**
 * Pixel-width metrics for Minecraft's default font, used to lay out written-book
 * pages so text fits the real on-screen page box instead of a naive character count.
 * <p>
 * A written book page is ~114px wide and shows 14 lines. Minecraft measures text by
 * rendered pixel width (a 'w' is far wider than an 'i'), and a signed book clips any
 * overflow instead of reflowing it. Wrapping by pixel width guarantees nothing is cut.
 * <p>
 * Widths are the default-font drawn widths; the renderer advances one extra pixel
 * of spacing per glyph, so effective width per char is {@code drawn + 1}. Unknown
 * glyphs fall back to a slightly generous width so we err towards wrapping early
 * (safe) rather than overflowing (clipped text).
 */
public final class BookFontMetrics {

    /** Usable pixel width of a written-book page line (vanilla is ~114; 113 leaves margin). */
    public static final int DEFAULT_PAGE_WIDTH = 113;

    /** Visible text lines per written-book page. */
    public static final int DEFAULT_LINES_PER_PAGE = 14;

    /** Spacing added after every glyph. */
    private static final int GLYPH_SPACING = 1;

    /** Drawn width of a space glyph (advance = SPACE_WIDTH + GLYPH_SPACING = 4). */
    public static final int SPACE_ADVANCE = 3 + GLYPH_SPACING;

    private static final int DEFAULT_DRAWN_WIDTH = 5;

    private BookFontMetrics() {}

    /**
     * @return the effective advance width (drawn + spacing) of a single character
     */
    public static int charWidth(char c) {
        return drawnWidth(c) + GLYPH_SPACING;
    }

    /**
     * @return the total pixel width of the given plain (unformatted) text
     */
    public static int width(String plain) {
        if (plain == null || plain.isEmpty()) return 0;
        int px = 0;
        for (int i = 0; i < plain.length(); i++) {
            px += charWidth(plain.charAt(i));
        }
        return px;
    }

    private static int drawnWidth(char c) {
        switch (c) {
            case ' ': return 3;
            case 'i': case '!': case ',': case '.': case ':': case ';':
            case '|': case '\'': return 1;
            case 'l': case '`': return 2;
            case 'I': case '[': case ']': case '"': case 't': return 3;
            case 'f': case 'k': case '(': case ')': case '{': case '}':
            case '<': case '>': return 4;
            case '@': case '~': return 6;
            default: return DEFAULT_DRAWN_WIDTH;
        }
    }
}
