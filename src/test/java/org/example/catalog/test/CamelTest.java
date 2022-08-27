package org.example.catalog.test;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.example.catalog.CatalogItem;
import org.example.catalog.test.camel.CamelSharedCatalog;
import org.example.catalog.test.mock.ParticipantTest;
import org.example.catalog.test.mock.SubjectTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;



public class CamelTest extends CamelTestSupport {

    private final static Logger LOGGER = LoggerFactory.getLogger(CamelTest.class);

    @Test
    public void test() throws Exception {

        final CamelSharedCatalog catalog = new CamelSharedCatalog(new ParticipantTest(UUID.randomUUID(), "owner"));
        context.getRegistry().bind("sharedCatalog", CamelSharedCatalog.class, catalog);

        Assertions.assertEquals(context.getStatus(),ServiceStatus.Started);
        final ProducerTemplate producer = context.createProducerTemplate();
        final ParticipantTest participant1 = new ParticipantTest(UUID.randomUUID(), "participant1");
        final List<CatalogItem<SubjectTest, ParticipantTest>> foreignItems = Arrays.asList(
//                new CatalogItemTest(Instant.ofEpochMilli(50), false, participant1, new ItemPayload("id1", "value1")),
//                new CatalogItemTest(Instant.ofEpochMilli(200), false, participant1, new ItemPayload("id2", "value2")),
//                new CatalogItemTest(Instant.ofEpochMilli(300), false, participant1, new ItemPayload("id3", "value3"))
        );
        foreignItems.forEach(item -> producer.sendBody("bean:sharedCatalog", item));
        getMockEndpoint("mock:test").setExpectedCount(3);
        assertMockEndpointsSatisfied();
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("seda:sharedCatalogOutbound")
                        .to("mock:test");
            }
        };
    }
}
