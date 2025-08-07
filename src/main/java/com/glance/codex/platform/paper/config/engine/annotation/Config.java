package com.glance.codex.platform.paper.config.engine.annotation;

import com.glance.codex.platform.paper.config.engine.ConfigController;
import com.glance.codex.platform.paper.config.engine.format.ConfigFormat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {
    /** config file path */
    String fileName() default "config";
    /** what kind of file this is (YAML by default) */
    ConfigFormat.Type format() default ConfigFormat.Type.YAML;
    /** top-level section (empty = root) */
    String section() default "";
    /** copy resource + write defaults on first load? */
    boolean writeDefaults() default true;
    /**
     * If true, will call JavaPlugin.saveDefaultConfig() and
     * load everything from plugin.getConfig()
     */
    boolean usePluginConfig() default false;

    /** Config Model Marker Interface */
    interface Handler {
        /**
         * Loads values from the config section into this instance
         * <p>
         * Does not write anything to disk
         */
        default void pull() {
            ConfigController.pull(this);
        }

        /**
         * Pushes this instances field values into the config section
         * <p>
         * Does not write to disk
         */
        default void push() {
            ConfigController.push(this);
        }

        /**
         * Reloads the config file into the stored section and pulls into the instance
         */
        default void reload() {
            ConfigController.reload(this);
        }

        /**
         * Writes the stored section related to this config instance to disk
         * @param push if true, pre-pushes the instance fields to the section before writing to disk
         */
        default void write(boolean push) {
            ConfigController.writeToDisk(this, push);
        }

        /**
         * Pushes current instance values into the related section, the writes to disk
         */
        default void save() {
            write(true);
        }
    }

}
