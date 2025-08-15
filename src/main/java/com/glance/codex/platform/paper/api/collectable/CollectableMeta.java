package com.glance.codex.platform.paper.api.collectable;

public record CollectableMeta(
    String namespace,
    String entryId,
    CollectableRepository repository
) {
}
