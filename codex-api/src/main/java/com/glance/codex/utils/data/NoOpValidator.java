package com.glance.codex.utils.data;

import java.util.Optional;

/**
 * Default no-op validator
 */
public class NoOpValidator implements Validator<Object> {
    @Override
    public Optional<String> validate(Object value) throws RuntimeException { return Optional.empty(); }
}
