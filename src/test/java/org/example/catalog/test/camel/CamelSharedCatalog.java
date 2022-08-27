package org.example.catalog.test.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Handler;
import org.apache.camel.ProducerTemplate;
import org.example.catalog.AckItem;
import org.example.catalog.CatalogItem;
import org.example.catalog.DefaultSharedCatalog;
import org.example.catalog.test.mock.ParticipantTest;
import org.example.catalog.test.mock.SubjectTest;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;

import static org.example.catalog.test.SharedCatalogTest.TEST_TOPIC;

public class CamelSharedCatalog extends DefaultSharedCatalog<SubjectTest, ParticipantTest> {
    public CamelSharedCatalog(ParticipantTest owner) {
        super(owner, Duration.ofMillis(100));
        topics.put(TEST_TOPIC, true);
        start();
    }

    @Handler
    public void onCamelMessage(CamelContext context, CatalogItem<SubjectTest, ParticipantTest> item) {
        acceptForeignCatalogItem(item);
        ProducerTemplate producer = context.createProducerTemplate();
        producer.sendBody("seda:sharedCatalogOutbound", item);
    }

    @Override
    protected Collection<CatalogItem<SubjectTest, ParticipantTest>> fetchMyItems() {
        return Arrays.asList(
//                new CatalogItemTest(Instant.ofEpochMilli(100), false, owner, new ItemPayload("id1", "value1")),
//                new CatalogItemTest(Instant.ofEpochMilli(200), false, owner, new ItemPayload("id2", "value2")),
//                new CatalogItemTest(Instant.ofEpochMilli(100), false, owner, new ItemPayload("id3", "value3"))
        );
    }

    @Override
    public void onAcknowledged(SubjectTest key, AckItem<ParticipantTest> ackItem) {

    }
}
