package org.example.catalog;

import java.time.Instant;

public record CatalogItemRecord<K extends Subject, T, V>
        (Instant date, boolean deleted, K subject, T owner, V payload)
        implements CatalogItem<K, T> { }


