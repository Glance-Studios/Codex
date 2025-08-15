package com.glance.codex.platform.paper.config.engine.exception;

/**
 * Thrown when a configuration file cannot be synced or saved
 */
public class ConfigSaveException extends RuntimeException {

    public ConfigSaveException(String message) {
        super(message);
    }

    public ConfigSaveException(String message, Throwable cause) {
        super(message, cause);
    }

}
