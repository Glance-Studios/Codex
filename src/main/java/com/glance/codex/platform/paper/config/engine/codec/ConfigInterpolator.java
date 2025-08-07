package com.glance.codex.platform.paper.config.engine.codec;

import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpolates variables in the form <code>${VAR_NAME[:default]}</code> using:
 * <ul>
 *     <li>System env variables</li>
 *     <li>System properties</li>
 *     <li>Default fallback</li>
 * </ul>
 *
 * @author Cammy
 */
@UtilityClass
public class ConfigInterpolator {

    private final Pattern PATTERN = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?}");

    /**
     * Replace all <code>${VAR_NAME[:default]}</code> in the input:
     *   <li>first tries {@link System#getenv(String)}</li>
     *   <li>then {@link System#getProperty(String)}</li>
     *   <li>then uses the provided default (if any)</li>
     *   <li>else throws {@link IllegalArgumentException}</li>
     */
    public String interpolate(String input) {
        Matcher m = PATTERN.matcher(input);
        StringBuilder out = new StringBuilder();

        while (m.find()) {
            String name = m.group(1);
            String def = m.group(2);

            String val = System.getenv(name);
            if (val == null) val = System.getProperty(name);

            if (val == null) {
                if (def != null) {
                    val = def;
                } else {
                    throw new IllegalArgumentException(
                            "Missing env variable or system property: " + name);
                }
            }

            m.appendReplacement(out, Matcher.quoteReplacement(val));
        }
        m.appendTail(out);
        return out.toString();
    }

}
