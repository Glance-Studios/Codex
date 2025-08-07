package com.glance.codex.platform.paper.command.engine.argument;

import io.leangen.geantyref.TypeToken;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.parser.ArgumentParser;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * Typed argument parser for Cloud v2.x that binds to a specific parsed type {@code T},
 * using a {@link TypeToken} for auto registration
 * </p>
 *
 * @param <T> the type parsed by this argument parser
 */
public interface TypedArgParser<T> extends ArgumentParser<CommandSender, T> {

    @NotNull
    TypeToken<T> typeToken();

    @SuppressWarnings("unchecked")
    default Class<T> type() {
        return (Class<T>) typeToken().getType();
    }

}
