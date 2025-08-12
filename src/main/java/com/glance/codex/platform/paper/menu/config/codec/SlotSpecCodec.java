package com.glance.codex.platform.paper.menu.config.codec;

import com.glance.codex.platform.paper.config.engine.codec.TypeCodec;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TypeCodec for {@link SlotSpec}
 * <p>
 * Accepts:
 * <ul>
 *     <li>String: "10-16,rows:2-3,row:6 col:5"</li>
 *     <li>List: [0, 1, "10-16", {row: 6, col: 5}]</li>
 *     <li>Map: {row: 6, col: 5} or {rows: "2-3"} or {cols: "1,3,5"}</li>
 * </ul>
 * <p>
 * All shapes are normalized into a single string expression
 *
 * @author Cammy
 */
public class SlotSpecCodec implements TypeCodec<SlotSpec> {

    @Override
    public @Nullable SlotSpec decode(ConfigurationSection section, String path, Type type, @Nullable SlotSpec defaultValue) {
        Object raw = section.get(path);
        return decodeFromRaw(raw, type, defaultValue);
    }

    @Override
    public SlotSpec decodeFromRaw(@Nullable Object raw, @NotNull Type type, @Nullable SlotSpec defaultValue) {
        String expr = Normalizer.toExpr(raw);
        return (expr != null) ? SlotSpec.of(expr) : defaultValue;
    }

    @Override
    public @Nullable Object encode(SlotSpec value) {
        return (value == null) ? null : value.getExpr();
    }

    private static final class Normalizer {
        private Normalizer(){}

        @Nullable
        static String toExpr(@Nullable Object raw) {
            if (raw == null) return null;
            if (raw instanceof SlotSpec spec) return spec.getExpr();
            if (raw instanceof CharSequence s) return s.toString().trim();
            if (raw instanceof Number n) return String.valueOf(n.intValue());
            if (raw.getClass().isArray()) {
                return fromCollection(List.of((Object[]) raw));
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
                    .collect(Collectors.joining(","));
            return joined.isBlank() ? null : joined;
        }

        static @Nullable String fromMap(Map<?, ?> m) {
            // {row: 6, col: 5}
            Object row = m.get("row");
            Object col = m.get("col");
            if (row instanceof Number r && col instanceof Number c) {
                return "row:" + r.intValue() + " col:" + c.intValue();
            }

            // {rows: "2-3"} or {rows: 2}
            Object rows = m.get("rows");
            if (rows instanceof CharSequence rs && !rs.toString().isBlank()) {
                return "rows:" + rs.toString().trim();
            }
            if (rows instanceof Number rn) {
                return "row:" + rn.intValue();
            }

            // {cols: "1,3,4"} or {cols: 3}
            Object cols = m.get("cols");
            if (cols instanceof CharSequence cs && !cs.toString().isBlank()) {
                return "cols:" + cs.toString().trim();
            }
            if (cols instanceof Number cn) {
                return "col:" + cn.intValue();
            }

            // No known shape present
            return null;
        }

    }

}
