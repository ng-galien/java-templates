package org.example.catalog;

import java.time.Instant;

public interface CatalogItem<K extends Subject, T> {

    K subject();

    T owner();

    boolean isDeleted();

    Instant getTimestamp();

    boolean isNewerThan(CatalogItem<K, T> otherItem);

}
