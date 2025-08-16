package com.glance.codex.utils.item;

public enum LoreMergeMode {
    REPLACE,  // Replace all existing lore
    APPEND,   // Append new lines to existing lore
    PREPEND,  // Prepend new lines before existing lore
    IGNORE_IF_PRESENT // Only set lore if none exists
}
