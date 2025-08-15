package com.glance.codex.platform.paper.text;

import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Locale;

@RequiredArgsConstructor
public class PluginPlaceholderSource implements PlaceholderSource {

    private final Plugin plugin;

    @Override
    public @Nullable String resolve(@NotNull String key) {
        switch (key.toLowerCase(Locale.ROOT)) {
            case "plugin.data":
                return plugin.getDataFolder().getAbsolutePath();
            case "plugin.name":
                return plugin.getName();
            case "plugin.dir": {
                try {
                    File jar = new File(plugin.getClass().getProtectionDomain()
                            .getCodeSource().getLocation().toURI());
                    return jar.getParentFile().getAbsolutePath();
                } catch (Exception ignored) {
                    return null;
                }
            }
            case "server.root":
                return new File(".").getAbsolutePath();
            default:
                return null;
        }
    }
}
