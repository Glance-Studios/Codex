package com.glance.codex.utils.io;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Simple file system helpers
 */
@UtilityClass
public class FileUtils {

    /**
     * Look in {@code folder} for any file named {@code baseName.ext} where
     * {@code ext} is one of {@code extensions}
     * @return the first match, or null
     */
    public File findFile(File folder, String baseName, List<String> extensions) {
        for (String ext : extensions) {
            File f = new File(folder, baseName + "." + ext);
            if (f.exists()) return f;
        }
        return null;
    }

    /**
     * Ensures a file exists at {@code folder/baseName.ext}. If it doesn't,
     * creates parent directories as needed and an empty file
     *
     * @param directory directory to contain the file
     * @param baseName file name without extension
     * @param ext extension (without dot)
     * @throws IOException if mkdirs() or createNewFile() fail
     */
    public File ensureFile(File directory, String baseName, String ext) throws IOException {
        return ensureResource(directory, baseName + "." + ext);
    }

    /**
     * Ensures a file exists at {@code folder/resourcePath}. If it doesn't,
     * creates parent directories as needed and an empty file
     *
     * @param directory directory to contain the file
     * @param resourcePath the full file name including extension if applicable
     * @throws IOException if mkdirs() or createNewFile() fail
     */
    public File ensureResource(File directory, String resourcePath) throws IOException {
        File f = new File(directory, resourcePath);
        if (!f.exists()) {
            File parent = f.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Could not create directories: " + parent);
            }
            if (!f.createNewFile()) {
                throw new IOException("Could not create file: " + f);
            }
        }
        return f;
    }

}
