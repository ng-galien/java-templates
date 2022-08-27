package org.example.catalog;

import java.time.Instant;

public record AckItemRecord<I>(boolean ok, boolean deleted, I by, Instant when) implements AckItem<I> { }
