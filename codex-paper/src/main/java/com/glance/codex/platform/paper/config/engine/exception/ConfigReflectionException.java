package com.glance.codex.platform.paper.config.engine.exception;

/**
 * Thrown when a configuration section and data holder sync causes an error during reflection
 */
public class ConfigReflectionException extends RuntimeException {

    public ConfigReflectionException(String message) {
        super(message);
    }

    public ConfigReflectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
