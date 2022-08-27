package org.example.catalog;

import java.time.Instant;
import java.util.Objects;

public abstract class DefaultCatalogItem<K extends Subject, T, V> implements CatalogItem<K, T> {

    private final Instant timestamp;

    private final boolean deleted;

    private final T owner;

    private final V payload;

    public DefaultCatalogItem(final Instant timestamp, final boolean deleted, final T owner, final V payload) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.deleted = Objects.requireNonNull(deleted, "deleted must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
    }

    @Override
    public T owner() {
        return owner;
    }

    public V getPayload() {
        return payload;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean isNewerThan(CatalogItem<K, T> otherItem) {
        return getTimestamp().isAfter(otherItem.getTimestamp());
    }
}
