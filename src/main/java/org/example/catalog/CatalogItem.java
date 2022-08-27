package org.example.catalog;

import java.time.Instant;
import java.time.temporal.TemporalAmount;

public interface CatalogItem<K extends Subject, T> {

    K subject();

    T owner();

    boolean deleted();

    Instant date();

    default boolean isNewerThan(CatalogItem<K, T> otherItem, TemporalAmount timeDelta) {
        return date().isAfter(otherItem.date().minus(timeDelta));
    }

}
