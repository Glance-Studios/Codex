package com.glance.codex.platform.paper.menu.config.codec;

import com.glance.codex.platform.paper.config.engine.codec.TypeCodec;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 * TypeCodec for {@link SlotSpec}
 *
 * Accepts:
 *  - String: "10-16,rows:2-3,row:6 col:5"
 *  - List:   [0, 1, "10-16", {row: 6, col: 5}]
 *  - Map:    {row: 6, col: 5} or {rows: "2-3"} or {cols: "1,3,5"} or {rows:"2-3", cols:"3-7"}
 *
 * All shapes are normalized into a single string expression
 */
public final class SlotSpecCodec implements TypeCodec<SlotSpec> {

    @Override
    public @Nullable SlotSpec decode(ConfigurationSection section, String path, Type type, @Nullable SlotSpec def) {
        return decodeFromRaw(section.get(path), type, def);
    }

    @Override
    public SlotSpec decodeFromRaw(@Nullable Object raw, @NotNull Type type, @Nullable SlotSpec def) {
        String expr = Normalizer.toExpr(raw);
        return (expr != null) ? SlotSpec.of(expr) : def;
    }

    @Override
    public @Nullable Object encode(SlotSpec value) {
        return (value == null) ? null : value.getExpr();
    }

    /* ---------------- Normalization helpers ---------------- */

    private static final class Normalizer {
        private Normalizer() {}

        @Nullable
        static String toExpr(@Nullable Object raw) {
            if (raw == null) return null;

            if (raw instanceof SlotSpec spec) return spec.getExpr();
            if (raw instanceof CharSequence s) return s.toString().trim();
            if (raw instanceof Number n) return String.valueOf(n.intValue());

            // Only handle object arrays; ignore primitive arrays
            if (raw instanceof Object[] arr) {
                return fromCollection(java.util.Arrays.asList(arr));
            }
            if (raw instanceof Collection<?> c) {
                return fromCollection(c);
            }
            if (raw instanceof Map<?, ?> m) {
                return fromMap(m);
            }
            return null;
        }

        static @Nullable String fromCollection(Collection<?> c) {
            if (c.isEmpty()) return "";
            String joined = c.stream()
                    .map(Normalizer::toExpr)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(java.util.stream.Collectors.joining(","));
            return joined.isBlank() ? null : joined;
        }

        static @Nullable String fromMap(Map<?, ?> m) {
            Object row  = m.get("row");
            Object rows = m.get("rows");
            Object col  = m.get("col");
            Object cols = m.get("cols");

            boolean hasRow = row != null || rows != null;
            boolean hasCol = col != null || cols != null;

            // Intersection: combine row(s) + col(s)
            if (hasRow && hasCol) {
                String rowExpr = (rows != null)
                        ? "rows:" + asRangeOrIndex(rows, true)
                        : "row:"  + asRangeOrIndex(row,  true);
                String colExpr = (cols != null)
                        ? "cols:" + asRangeOrIndex(cols, false)
                        : "col:"  + asRangeOrIndex(col,  false);

                if (rowExpr.endsWith(":") || colExpr.endsWith(":")) return null;
                return rowExpr + " " + colExpr;
            }

            // Rows only
            if (rows != null) { String v = asRangeOrIndex(rows, true);  return v.isEmpty() ? null : "rows:" + v; }
            if (row  != null) { String v = asRangeOrIndex(row,  true);  return v.isEmpty() ? null : "row:"  + v; }

            // Cols only
            if (cols != null) { String v = asRangeOrIndex(cols, false); return v.isEmpty() ? null : "cols:" + v; }
            if (col  != null) { String v = asRangeOrIndex(col,  false); return v.isEmpty() ? null : "col:"  + v; }

            return null;
        }

        /**
         * Accept either a number or a string for row/rows/col/cols
         * - Numbers become simple indices (e.g., 6 -> "6")
         * - CharSequence is returned trimmed (e.g., "2-3", "3,5,7", "last", "-1")
         *   'last' / '-1' are allowed only for rows; cols ignore those words
         */
        private static String asRangeOrIndex(Object v, boolean allowLastForRow) {
            if (v instanceof Number n) return String.valueOf(n.intValue());
            if (v instanceof CharSequence cs) {
                String s = cs.toString().trim();
                if (s.isEmpty()) return "";
                if (!allowLastForRow) {
                    // For columns, do not carry special words; keep numeric/range strings only
                    return s;
                }
                // rows: allow "last" / "-1" to pass through; parser will interpret
                return s;
            }
            return "";
        }
    }
}
