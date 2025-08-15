package com.glance.codex.platform.paper.config.engine.exception;

import com.glance.codex.utils.data.Validator;

/**
 * Exception thrown when config validation fails due to a missing or invalid path value
 * <p>
 * Typically raised by or a custom {@link Validator}
 * </p>
 * @author AviaraTeam
 */
public class ConfigValidationException extends RuntimeException {

    /**
     * Constructs a new ConfigValidationException with the specified detail messaging
     *
     * @param message the detail messaging explaining the cause of the validation failure
     */
    public ConfigValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new ConfigValidationException with the specified detail messaging and cause
     *
     * @param message the detail messaging explaining the cause of the validation failure
     * @param cause   the underlying exception that triggered this validation failure
     */
    public ConfigValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
