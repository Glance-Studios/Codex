package com.glance.codex.platform.paper.config.engine.codec;

import com.glance.codex.platform.paper.text.PlaceholderSource;
import com.glance.codex.platform.paper.text.PluginPlaceholderSource;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpolates variables in the form <code>${VAR_NAME[:default]}</code> using:
 * <ul>
 *     <li>System env variables</li>
 *     <li>System properties</li>
 *     <li>Registered placeholder sources</li>
 *     <li>Default fallback</li>
 * </ul>
 *
 * @author Cammy
 */
@Slf4j
@UtilityClass
public class ConfigInterpolator {

    private final Pattern PATTERN = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?}");

    private final List<PlaceholderSource> GLOBAL_SOURCES = new CopyOnWriteArrayList<>();

    /**
     * Register a global placeholder source
     */
    public void registerSource(@NotNull PlaceholderSource source) {
        GLOBAL_SOURCES.add(source);
    }

    /**
     * Replace all ${VAR_NAME[:default]} in the input using env -> sysprops -> global sources -> default
     * <p>
     * Unresolved tokens without default are left as-is
     */
    public String interpolate(@NotNull String input) {
        return interpolate(input, GLOBAL_SOURCES);
    }

    /**
     * Convenience overload for a single-use Plugin context
     */
    public String interpolate(@NotNull String input, @NotNull Plugin plugin) {
        return interpolate(input, List.of(new PluginPlaceholderSource(plugin)));
    }

    /**
     * Replace all <code>${VAR_NAME[:default]}</code> in the input:
     *   <li>first tries {@link System#getenv(String)}</li>
     *   <li>then {@link System#getProperty(String)}</li>
     *   <li>then uses the provided default (if any)</li>
     *   <li>else throws {@link IllegalArgumentException}</li>
     */
    public String interpolate(String input, @NotNull List<PlaceholderSource> sources) {
        Matcher m = PATTERN.matcher(input);
        StringBuilder out = new StringBuilder();
        boolean warned = false;

        while (m.find()) {
            String name = m.group(1);
            String def = m.group(2);

            String val = System.getenv(name);
            if (val == null) val = System.getProperty(name);

            if (val == null) {
                for (PlaceholderSource s : sources) {
                    val = s.resolve(name);
                    if (val != null) break;
                }
            }

            if (val == null) {
                if (def != null) {
                    val = def;
                } else {
                    if (!warned) {
                        log.warn("Missing placeholder(s) with no default while interpolating: first missing='{}'", name);
                        warned = true;
                    }
                    val = m.group(0);
                }
            }

            m.appendReplacement(out, Matcher.quoteReplacement(val));
        }
        m.appendTail(out);
        return out.toString();
    }

}
