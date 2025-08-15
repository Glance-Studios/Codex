package com.glance.codex.platform.paper.config.engine.codec;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Functional interface for dynamically resolving {@link TypeCodec} instances
 * based on runtime {@link Type}
 * <p>
 * This allows codecs to be provided on demand for types that are not known ahead of time
 * <p>
 * Registered via {@link CodecRegistry#registerResolver(CodecResolver)}
 *
 * @author Cammy
 */
@FunctionalInterface
public interface CodecResolver {
    @Nullable
    TypeCodec<?> resolve(Type type);
}
