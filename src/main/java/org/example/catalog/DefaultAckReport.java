package org.example.catalog;


import java.util.Map;

public class DefaultAckReport<K, T> implements AckReport<K, T> {
    private final boolean ok;
    private final Map<K, AckItem<T>> items;

    public DefaultAckReport(boolean ok, Map<K, AckItem<T>> items) {
        this.ok = ok;
        this.items = items;
    }

    @Override
    public boolean ok() {
        return ok;
    }

    @Override
    public Map<K, AckItem<T>> getItems() {
        return items;
    }
}
