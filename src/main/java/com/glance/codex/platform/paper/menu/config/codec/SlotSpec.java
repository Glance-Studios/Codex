package com.glance.codex.platform.paper.menu.config.codec;

import com.glance.codex.utils.format.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;

/**
 * Describes a set of GUI slot positions using various forms of a string expression,
 * resolving into concrete slot indices for a given inventory height (rows)
 * <p>
 * Grammar (case-insensitive, comma-separated tokens)
 * </p>
 * <ul>
 *   <li><b>Single slot</b>: {@code 0}, {@code 5}, {@code 22}</li>
 *   <li><b>Range</b>: {@code 10-16}</li>
 *   <li><b>Row(s)</b>: {@code row:2} or {@code rows:2-3} (1-based rows)</li>
 *   <li><b>Column(s)</b>: {@code col:3} or {@code cols:1-2} (1-based columns)</li>
 *   <li><b>Specific cell</b>: {@code row:6 col:5} (1-based row/column)</li>
 * </ul>
 *
 * <b>BitSet</b> used to dedupe slots efficiently and keep parsing order-agnostic</b>
 *
 * @author Cammy
 */
@Getter
@EqualsAndHashCode
public final class SlotSpec {

    /** Original expression as written in config */
    private final String expr; // string expression of the slot spec

    private SlotSpec(@Nullable String expr) {
        this.expr = (expr == null) ? "" : expr.trim();
    }

    public static SlotSpec of(@Nullable String expr) {
        return new SlotSpec(expr);
    }

    /**
     * Resolve this spec to concrete slot indices for a GUI with the given row count
     * <p>
     * Rows are clamped to {@code [1,6]} and the resulting slots are sorted, unique,
     * and within {@code 0..rows*9-1}
     *
     * @param rows number of inventory rows (1..6)
     * @return list of slot indices suitable for Bukkit inventory APIs
     */
    public List<Integer> resolve(int rows) {
        return parse(expr, rows);
    }

    @Override
    public String toString() { return expr; }

    /*
    * ===============
    * Internal Parser
    * ===============
    */

    private static List<Integer> parse(@Nullable String spec, int rows) {
        if (StringUtils.isNullOrBlank(spec)) return List.of(); assert spec != null;

        // Clamp to valid GUI heights
        final int rowCount = Math.max(1, Math.min(6, rows));
        final int size = rowCount * 9;

        BitSet bs = new BitSet(size);
        for (String token : spec.split(",")) {
            String t = token.trim().toLowerCase(Locale.ROOT);
            if (t.isEmpty()) continue;

            // Specific cell: "row:X col:Y"
            if (t.contains("row:") && t.contains("col:")) {
                int r = getIndexFromKey(t, "row:");
                int c = getIndexFromKey(t, "col:");
                int slot = rowColToSlot(r, c, size);
                if (slot >= 0) bs.set(slot);
                continue;
            }

            // Full rows: "row:2" or "rows:2-3"
            if (t.startsWith("row:") || t.startsWith("rows:")) {
                String range = t.substring(t.indexOf(':') + 1);
                for (int r : parseRange(range)) {
                    if (r < 1 || r > size / 9) continue;
                    int start = (r - 1) * 9;
                    bs.set(start, start + 9);
                }
                continue;
            }

            // Full columns: "col:3" or "cols:1-2"
            if (t.startsWith("col:") || t.startsWith("cols:")) {
                String range = t.substring(t.indexOf(':') + 1);
                for (int c : parseRange(range)) {
                    if (c < 1 || c > 9) continue;
                    int col = c - 1;
                    for (int r = 0; r < rowCount; r++) bs.set(r * 9 + col);
                }
            }

            // Numeric range "10-16" or single "5" etc
            if (t.contains("-")) {
                String[] ab = t.split("-", 2);
                int a = parseInt(ab[0], 1), b = parseInt(ab[1], -1);
                if (a >= 0 && b >= 0) {
                    int low = Math.min(a, b), high = Math.max(a, b);
                    for (int i = low; i <= high && i < size; i++) bs.set(i);
                }
            } else {
                int i = parseInt(t, -1);
                if (i >= 0 && i < size) bs.set(i);
            }
        }

        List<Integer> out = new ArrayList<>(bs.cardinality());
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) out.add(i);
        return out;
    }

    private static int rowColToSlot(int row, int col, int size) {
        int rows = size / 9;
        if (row < 1 || row > rows || col < 1 || col > 9) return -1;
        return (row - 1) * 9 + (col - 1);
    }

    /**
     * Extract a single integer that follows a key within a token,
     * e.g. {@code singleIndex("row:6 col:5", "row:") == 6}.
     */
    private static int getIndexFromKey(String t, String key) {
        int idx = t.indexOf(key); if (idx < 0) return -1;
        int from = idx + key.length();
        int to = t.indexOf(' ', from);
        String num = (to < 0) ? t.substring(from) : t.substring(from, to);
        return parseInt(num, -1);
    }

    /** Parse an integer with a safe default */
    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Parse a {@code row}/{@code col} range like "2-4" or a single "3" into an int array
     * <p>
     * Values are not clamped here; validate according to context
     */
    private static int[] parseRange(String range) {
        String r = range.trim();
        if (r.contains("-")) {
            String[] ab = r.split("-", 2);
            int a = parseInt(ab[0], -1), b = parseInt(ab[1], -1);
            if (a < 0 || b < 0) return new int[0];

            int low = Math.max(a, b), high = Math.max(a, b);
            int[] out = new int[high - low + 1];
            for (int i = 0, val = low; val <= high; val++, i++) out[i] = val;
            return out;
        }
        int val = parseInt(r, -1);
        return (val < 0) ? new int[0] : new int[]{val};
    }

}
