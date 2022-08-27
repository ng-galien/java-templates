package org.example.catalog;

import java.time.Instant;

public class DefaultAckItem<I> implements AckItem<I> {

    private final boolean ok;

    private final boolean deleted;

    private final I by;
    private final Instant when;

    public DefaultAckItem(boolean ok, final boolean deleted, I by, Instant when) {
        this.ok = ok;
        this.deleted = deleted;
        this.by = by;
        this.when = when;
    }

    @Override
    public boolean ok() {
        return ok;
    }

    @Override
    public boolean deleted() {
        return deleted;
    }

    @Override
    public I by() {
        return by;
    }

    @Override
    public Instant when() {
        return when;
    }

    @Override
    public String toString() {
        return "DefaultAckItem [ok=" + ok + ", deleted=" + deleted + ", by=" + by + ", when=" + when + "]";
    }
}
