package com.glance.codex.api.collectable.config.model;

public interface LineWrapConfig {

    default Integer maxLineLength() {
        return -1;
    }

    default boolean breakWords() {
        return false;
    }

}
