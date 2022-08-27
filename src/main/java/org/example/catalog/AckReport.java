package org.example.catalog;

import java.util.Map;

public interface AckReport<K, T> {

    boolean ok();

    Map<K, AckItem<T>> getItems();

}
