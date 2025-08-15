package com.glance.codex.api.text;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Core API for performing token-based replacements on strings
 * <p>
 * Supports:
 * <ul>
 *   <li>Locally specified, per call placeholders ({@code {key} -> value})</li>
 *   <li>Globally registered dynamic tokens ({@code {key} -> resolver.apply(player)})</li>
 *   <li>Optional integration with PlaceholderAPI (%...% expansions) if present on the classpath</li>
 * </ul>
 * Thread safety: implementations should guard their global registry against concurrent access
 * </p>
 * @author Cammy
 */
public interface PlaceholderService {

    /**
     * Globally register a dynamic token
     * <p>
     * Whenever {@code apply(...)} is called on a template containing
     * <code>{key}</code>, the provided {@code resolver} will be invoked
     * (with the given {@link OfflinePlayer}, which may be {@code null})
     * to compute its replacement
     * </p>
     *
     * @param key the token name (without braces), must be non-null
     * @param resolver mapping from {@link OfflinePlayer} (or {@code null})
     *                  to the desired replacement string; must be non-null
     * @throws NullPointerException if {@code key} or {@code resolver} is null
     */
    void registerDynamic(String key, Function<@Nullable OfflinePlayer, String> resolver);

    /**
     * Register a purely static token: always invokes the supplier (no player context)
     *
     * @param key       the token name (without braces)
     * @param supplier  provides the replacement whenever {key} is encountered
     * @see #registerDynamic(String, Function)
     */
    default void registerDynamic(String key, Supplier<String> supplier) {
        registerDynamic(key, player -> supplier.get());
    }

    /**
     * Remove a previously registered dynamic token
     * <p>
     * After this call, {@code {key}} will no longer be replaced by its
     * resolver function
     * </p>
     *
     * @param key the token name to unregister (without braces)
     */
    void unregisterDynamic(String key);

    /**
     * Obtain an immutable view of all currently registered global tokens
     *
     * @return a {@link Set} of token names (without braces)
     */
    Set<String> listRegistered();

    /**
     * Perform placeholder replacements on a single string
     * <p>
     * The replacement happens in three phases:
     * <ol>
     *   <li>All entries in {@code locals} are substituted for
     *       <code>{key}</code> in the template</li>
     *   <li>All globally registered tokens are applied, in registration order</li>
     *   <li>If PlaceholderAPI is present on the classpath and
     *       {@code player != null}, the result is passed to
     *       {@code PlaceholderAPI.setPlaceholders(player, result)}</li>
     * </ol>
     * </p>
     *
     * @param template the raw text containing zero or more
     *                  <code>{key}</code> or <code>%papi%</code> tokens;
     *                  must be non-null
     * @param player optional context for both global resolvers and
     *                  PlaceholderAPI; may be {@code null} to skip PAPI
     * @param locals ad-hoc replacements for this invocation (key -> value);
     *                  must be non-null (empty map if none)
     * @return the fully processed string
     * @throws NullPointerException if {@code template} or {@code locals} is null
     */
    String apply(String template,
                 @Nullable OfflinePlayer player,
                 Map<String, String> locals);

    /**
     * Convenience overload: perform replacements using varargs for locals
     * <p>
     * Example:
     * <pre>{@code
     * apply("Hello {name}, you have {coins} coins", player,
     *       "name", "Cammy", "coins", "42");
     * }</pre>
     * </p>
     *
     * @param template the raw template (see {@link #apply(String, OfflinePlayer, Map)})
     * @param player optional player context for globals + PAPI
     * @param keyVals an even-length array of key/value pairs
     * @return the processed string
     * @throws IllegalArgumentException if {@code keyVals.length} is odd
     */
    default String apply(String template,
                         @Nullable OfflinePlayer player,
                         String... keyVals
    ) {
        if (keyVals.length % 2 != 0) {
            throw new IllegalArgumentException("Must pass an even number of arguments");
        }
        Map<String, String> locals = new LinkedHashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            locals.put(keyVals[i], keyVals[i+1]);
        }
        return apply(template, player, locals);
    }

    /**
     * Apply placeholders to every string in the given list
     * <p>
     * Each element is processed through {@link #apply(String, OfflinePlayer, Map)}
     * </p>
     *
     * @param templates the list of raw templates; must be non-null
     * @param player optional player context for globals + PAPI
     * @param locals ad-hoc replacements; must be non-null
     * @return a new list of processed strings
     * @throws NullPointerException if {@code templates} or {@code locals} is null
     */
    default List<String> apply(List<String> templates,
                               @Nullable OfflinePlayer player,
                               Map<String, String> locals)
    {
        return templates.stream()
                .map(t -> apply(t, player, locals))
                .toList();
    }

    /**
     * Shorthand for list + player, no locals
     *
     * @param templates the list of raw templates
     * @param player optional player context
     * @return processed list
     * @see #apply(List, OfflinePlayer, Map)
     */
    default List<String> apply(List<String> templates,
                               @Nullable OfflinePlayer player)
    {
        return apply(templates, player, Collections.emptyMap());
    }

    /**
     * Shorthand for list + locals, no player
     *
     * @param templates the list of raw templates
     * @param locals ad-hoc replacements
     * @return processed list (PAPI skipped)
     * @see #apply(List, OfflinePlayer, Map)
     */
    default List<String> apply(List<String> templates,
                               Map<String, String> locals)
    {
        return apply(templates, null, locals);
    }

    /**
     * Shorthand for list only (globals only, no PAPI, no locals)
     *
     * @param templates the list of raw templates
     * @return processed list
     * @see #apply(List, OfflinePlayer, Map)
     */
    default List<String> apply(List<String> templates)
    {
        return apply(templates, null, Collections.emptyMap());
    }

    /**
     * Shorthand for (globals only, no PAPI, no locals)
     *
     * @param template the raw string template
     * @return processed list
     * @see #apply(List, OfflinePlayer, Map)
     */
    default String apply(String template) {
        return apply(template, null);
    }

    /**
     * Shorthand: no locals
     *
     * @param template the raw template
     * @param player optional player context
     * @return processed string
     * @see #apply(String, OfflinePlayer, Map)
     */
    default String apply(String template, @Nullable OfflinePlayer player) {
        return apply(template, player, Collections.emptyMap());
    }

    /**
     * Convenience overload for lists with varargs locals + player
     *
     * @param templates the list of raw templates
     * @param player optional player context
     * @param keyVals even-length array of key/value pairs
     * @return processed list
     * @throws IllegalArgumentException if {@code keyVals.length} is odd
     * @see #apply(List, OfflinePlayer, Map)
     */
    default List<String> apply(List<String> templates,
                               @Nullable OfflinePlayer player,
                               String... keyVals)
    {
        if (keyVals.length % 2 != 0) {
            throw new IllegalArgumentException("Must pass an even number of arguments");
        }
        Map<String, String> locals = new LinkedHashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            locals.put(keyVals[i], keyVals[i + 1]);
        }
        return apply(templates, player, locals);
    }

    /** Convenience: pass a real online Player (no casting) */
    default String apply(String template, Player player, Map<String,String> locals) {
        return apply(template, (OfflinePlayer) player, locals);
    }

    /** Convenience: pass a real online Player (no casting) but no locals */
    default String apply(String template, Player player) {
        return apply(template, (OfflinePlayer) player, Collections.emptyMap());
    }

    /** Convenience: pass a real online Player (no casting) */
    default List<String> apply(List<String> templates, Player player, Map<String,String> locals) {
        return apply(templates, (OfflinePlayer) player, locals);
    }

    /** Convenience: pass a real online Player (no casting) but no locals */
    default List<String> apply(List<String> templates, Player player) {
        return apply(templates, (OfflinePlayer) player, Collections.emptyMap());
    }

}
