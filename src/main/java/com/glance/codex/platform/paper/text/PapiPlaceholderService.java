package com.glance.codex.platform.paper.text;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PapiPlaceholderService extends DefaultPlaceholderService {

    private final boolean bracketEnabled;
    private final boolean percentEnabled;

    public PapiPlaceholderService() {
        this.bracketEnabled = false;
        this.percentEnabled = true;
    }

    public PapiPlaceholderService(boolean bracketEnabled, boolean percentEnabled) {
        this.bracketEnabled = bracketEnabled;
        this.percentEnabled = percentEnabled;
    }

    @Override
    public String apply(String template,
                        @Nullable OfflinePlayer player,
                        Map<String,String> locals
    ) {
        String result = super.apply(template, player, locals);

        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            if (bracketEnabled) {
                result = PlaceholderAPI.setBracketPlaceholders(player, result);
            }
            if (percentEnabled) {
                result = PlaceholderAPI.setPlaceholders(player, result);
            }
        }
        return result;
    }
}
