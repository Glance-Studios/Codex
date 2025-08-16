package com.glance.codex.api.text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PlaceholderSource {
    /**
     * @return resolved value or null if this source doesn't handle it
     */
    @Nullable
    String resolve(@NotNull String key);
}
