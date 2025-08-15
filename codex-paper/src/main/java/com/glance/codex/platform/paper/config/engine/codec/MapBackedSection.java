package com.glance.codex.platform.paper.config.engine.codec;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.Map;

/**
 * Wraps a raw {@code Map<String, Object} into a fake {@link ConfigurationSection}
 * for compatibility with Bukkit config deserialization
 * <p>
 * Used to simulate reading config data from memory during record construction
 *
 * @author Cammy
 */
public class MapBackedSection extends MemoryConfiguration {
    MapBackedSection(Map<String,Object> map) {
        super(null);
        for (var e : map.entrySet()) {
            this.set(e.getKey(), e.getValue());
        }
    }
}
