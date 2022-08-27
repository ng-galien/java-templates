package org.example.catalog.test.mock;

import org.example.catalog.DefaultCatalogItem;

import java.time.Instant;

import static org.example.catalog.test.SharedCatalogTest.TEST_TOPIC;

public class CatalogItemTest extends DefaultCatalogItem<SubjectTest, ParticipantTest, ItemPayload> {

    private final SubjectTest subject;

    public CatalogItemTest(Instant timestamp, boolean deleted, ParticipantTest owner, ItemPayload payload) {
        super(timestamp, deleted, owner, payload);
        subject = new SubjectTest(TEST_TOPIC, payload.id());
    }

    @Override
    public String toString() {
        return "CatalogItemTest{" +
                "timestamp=" + getTimestamp() +
                ", deleted=" + isDeleted() +
                ", value='" + getPayload() + '\'' +
                '}';
    }

    @Override
    public SubjectTest subject() {
        return subject;
    }
}

