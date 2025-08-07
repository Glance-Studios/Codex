package com.glance.codex.platform.paper.text;

import com.glance.codex.platform.paper.api.text.PlaceholderService;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Default implementation of {@link PlaceholderService}
 * <p>
 * Maintains a registry of global dynamic tokens and processes templates in three phases:
 * <ol>
 *   <li>Apply per call locals ({@code {key} -> value})</li>
 *   <li>Apply registered globals ({@code {key} -> resolver.apply(player)})</li>
 *   <li>If PlaceholderAPI is on the classpath and player != null, invoke {@code PlaceholderAPI.setPlaceholders(player, str)}</li>
 * </ol>
 * Thread safe: readers (apply/listRegistered) acquire a read-lock; writers (registerDynamic/unregisterDynamic) acquire a write-lock
 * </p>
 * @author Cammy
 */
public class DefaultPlaceholderService implements PlaceholderService {

    private final LinkedHashMap<String, Function<@Nullable OfflinePlayer, String>> globals = new LinkedHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    @Override
    public void registerDynamic(String key, Function<@Nullable OfflinePlayer, String> resolver) {
        lock.writeLock().lock();
        try { globals.put(key, resolver); }
        finally { lock.writeLock().unlock(); }
    }

    @Override
    public void unregisterDynamic(String key) {
        lock.writeLock().lock();
        try { globals.remove(key); }
        finally { lock.writeLock().unlock(); }
    }

    @Override
    public Set<String> listRegistered() {
        lock.readLock().lock();
        try { return Collections.unmodifiableSet(new LinkedHashSet<>(globals.keySet())); }
        finally { lock.readLock().unlock(); }
    }

    @Override
    public String apply(String template, @Nullable OfflinePlayer player, Map<String, String> locals) {
        String result = template;
        // locals
        for (var e : locals.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue());
        }
        // globals
        lock.readLock().lock();
        try {
            for (var e : globals.entrySet()) {
                result = result.replace("{" + e.getKey() + "}", e.getValue().apply(player));
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }
}
