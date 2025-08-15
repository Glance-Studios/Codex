package com.glance.codex.api.collectable;

public record CollectableMeta(
    String namespace,
    String entryId,
    CollectableRepository repository
) {
}
