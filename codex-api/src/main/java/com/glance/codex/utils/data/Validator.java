package com.glance.codex.utils.data;

import java.util.Optional;

/**
 * Validates values of type T
 *
 * @param <T> the type of the value to validate
 */
@FunctionalInterface
public interface Validator<T> {
    /**
     * Validate the given value
     *
     * @param value the value to check
     * @return Optional.empty() if valid, or Optional.of(errorMessage) if invalid
     */
    Optional<String> validate(T value);
}
