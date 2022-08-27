package org.example.catalog;

import java.time.Instant;

public interface AckItem<I> {

    boolean ok();

    boolean deleted();

    I by();

    Instant when();

}
