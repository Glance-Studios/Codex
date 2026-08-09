package com.glance.codex.platform.paper.notebooks.edit;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Where a registered book came from on disk
 * <p>
 * Recorded at registration so an in game edit writes back to exactly the key it was loaded
 * from, rather than re-deriving a file from the namespace and hoping it matches
 *
 * @param file the notes config file holding this book
 * @param yamlPath section path of the book within that file, e.g. {@code books.lost_grimoire}
 *                 for the multi book form, or {@code book} for the single book form
 *
 * @author Cammy
 */
public record BookSource(@NotNull Path file, @NotNull String yamlPath) {

    /** Path of a field inside this book's section, e.g. {@code books.lost_grimoire.content} */
    public @NotNull String pathOf(@NotNull String field) {
        return yamlPath + "." + field;
    }

}
