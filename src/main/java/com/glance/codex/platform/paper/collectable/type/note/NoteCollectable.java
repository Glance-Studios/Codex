package com.glance.codex.platform.paper.collectable.type.note;

import com.glance.codex.platform.paper.api.collectable.base.BaseCollectable;
import com.glance.codex.platform.paper.config.engine.annotation.ConfigField;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NoteCollectable extends BaseCollectable {

    @ConfigField
    private String bookId;

    @Override
    public void onDiscover(@NotNull Player player) {
        super.onDiscover(player);
    }

    @Override
    public void onReplay(@NotNull Player player) {
        super.onReplay(player);
    }

}
