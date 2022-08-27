package org.example.catalog;


import java.util.Map;

public record AckReportRecord<K, T>(boolean ok, Map<K, AckItem<T>> items) implements AckReport<K, T> {
}
