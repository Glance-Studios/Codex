package com.glance.codex.utils.format;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class StringUtils {

    public boolean isNullOrBlank(@Nullable String string) {
        return string == null || string.isBlank() || string.isEmpty();
    }

}
