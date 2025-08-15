package com.glance.codex.platform.paper.config.engine.exception;

/**
 * Thrown when a configuration file cannot be loaded, parsed, or bound
 */
public class ConfigLoadException extends RuntimeException {

    public ConfigLoadException(String message) {
        super(message);
    }

    public ConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }

}
