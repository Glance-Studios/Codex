package com.glance.codex;

import com.glance.codex.api.collectable.CollectableAPI;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

@UtilityClass
public class Codex {

    public CollectableAPI api() {
        return Bukkit.getServicesManager().load(CollectableAPI.class);
    }

}
