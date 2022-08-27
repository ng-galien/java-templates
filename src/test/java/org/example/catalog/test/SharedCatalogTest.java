package org.example.catalog.test;

import org.example.catalog.*;
import org.example.catalog.test.mock.CatalogItemTest;
import org.example.catalog.test.mock.ItemPayload;
import org.example.catalog.test.mock.ParticipantTest;
import org.example.catalog.test.mock.SubjectTest;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;


public class SharedCatalogTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(SharedCatalogTest.class);

    private SharedCatalog<SubjectTest, ParticipantTest> sharedCatalog;

    private final ParticipantTest owner = new ParticipantTest(UUID.randomUUID(), "owner");

    public static final String TEST_TOPIC = "testTopic";
    private static final String TEST_SEPARATOR = "=========================== {} ===============================";

    @BeforeEach
    void setUp(TestInfo testInfo) {
        LOGGER.info(TEST_SEPARATOR, testInfo.getDisplayName());
    }
    @AfterEach
    void tearDown(TestInfo testInfo) {
        LOGGER.info(TEST_SEPARATOR, "END");
        System.out.println("");
    }

    @BeforeEach
    public void setUp() {
        sharedCatalog = new DefaultSharedCatalog<>(owner) {

            {
                topics.put(TEST_TOPIC, true);
            }

            @Override
            public void onAcknowledged(SubjectTest topic, AckItem<ParticipantTest> ackItem) {
                LOGGER.warn("onAcknowledged: topic={}, ackItem={}", topic, ackItem);
            }

            @Override
            protected Collection<CatalogItem<SubjectTest, ParticipantTest>> fetchMyItems() {
                return Arrays.asList(
                        new CatalogItemTest(Instant.ofEpochMilli(100), false, owner, new ItemPayload("id1", "value1")),
                        new CatalogItemTest(Instant.ofEpochMilli(200), false, owner, new ItemPayload("id2", "value2")),
                        new CatalogItemTest(Instant.ofEpochMilli(100), false, owner, new ItemPayload("id3", "value3"))
                );
            }
        };
    }

    @Test
    public void testItemIsSame() {
        CatalogItemTest item1 = new CatalogItemTest(Instant.ofEpochMilli(100), false, owner, new ItemPayload("id1", "value1"));
        CatalogItemTest item2 = new CatalogItemTest(Instant.ofEpochMilli(100), false, owner, new ItemPayload("id1", "value1"));
        Assertions.assertFalse(item2.isNewerThan(item1));
    }

    @Test
    public void testItemIsNewerThan() {
        CatalogItemTest item1 = new CatalogItemTest(Instant.ofEpochMilli(100), false, owner, new ItemPayload("id1", "value1"));
        CatalogItemTest item2 = new CatalogItemTest(Instant.ofEpochMilli(200), false, owner, new ItemPayload("id1", "value1"));
        Assertions.assertTrue(item2.isNewerThan(item1));
    }

    @Test
    public void testItemIsNotNewerThan() {
        CatalogItemTest item1 = new CatalogItemTest(Instant.ofEpochMilli(100), false, owner, new ItemPayload("id1", "value1"));
        CatalogItemTest item2 = new CatalogItemTest(Instant.ofEpochMilli(200), false, owner, new ItemPayload("id1", "value1"));
        Assertions.assertFalse(item1.isNewerThan(item2));
    }

    @Test
    public void testTwoParticipantUpdated() {
        sharedCatalog.start();
        
        final ParticipantTest participant1 = new ParticipantTest(UUID.randomUUID(), "participant1");
        final List<CatalogItem<SubjectTest, ParticipantTest>> foreignItems = Arrays.asList(
                new CatalogItemTest(Instant.ofEpochMilli(50), false, participant1, new ItemPayload("id1", "value1")),
                new CatalogItemTest(Instant.ofEpochMilli(200), false, participant1, new ItemPayload("id2", "value2")),
                new CatalogItemTest(Instant.ofEpochMilli(300), false, participant1, new ItemPayload("id3", "value3"))
        );
        foreignItems.forEach( item -> sharedCatalog.acceptForeignCatalogItem(item));
        sharedCatalog.acknowledgeReceivedItem(new CatalogItemTest(Instant.ofEpochMilli(300), false, participant1, new ItemPayload("id3", "value3")));
        Assertions.assertTrue(sharedCatalog.acknowledged());

        final AckReport<SubjectTest, ParticipantTest> ackReport = sharedCatalog.getAckReport();
        Assertions.assertEquals(1, ackReport.getItems().size());
        printReport(ackReport);
    }

    @Test
    public void testTwoParticipantNewer() {
        sharedCatalog.start();
        final ParticipantTest participant1 = new ParticipantTest(UUID.randomUUID(), "participant1");
        final List<CatalogItem<SubjectTest, ParticipantTest>> foreignItems = Arrays.asList(
                new CatalogItemTest(Instant.ofEpochMilli(50), false, participant1, new ItemPayload("id1", "value1")),
                new CatalogItemTest(Instant.ofEpochMilli(200), false, participant1, new ItemPayload("id2", "value2")),
                new CatalogItemTest(Instant.ofEpochMilli(100), false, participant1, new ItemPayload("id4", "value4"))
        );
        foreignItems.forEach( item -> sharedCatalog.acceptForeignCatalogItem(item));
        sharedCatalog.acknowledgeReceivedItem(new CatalogItemTest(Instant.ofEpochMilli(300), false, participant1, new ItemPayload("id4", "value4")));
        Assertions.assertTrue(sharedCatalog.acknowledged());
        Assertions.assertEquals(1, sharedCatalog.getItemsToShare().size());

        final AckReport<SubjectTest, ParticipantTest> ackReport = sharedCatalog.getAckReport();
        Assertions.assertEquals(1, ackReport.getItems().size());
        printReport(ackReport);
    }

    @Test
    public void testTwoParticipantDelete() {
        sharedCatalog.start();
        final ParticipantTest participant1 = new ParticipantTest(UUID.randomUUID(), "participant1");
        final List<CatalogItem<SubjectTest, ParticipantTest>> foreignItems = Arrays.asList(
                new CatalogItemTest(Instant.ofEpochMilli(50), false, participant1, new ItemPayload("id1", "value1")),
                new CatalogItemTest(Instant.ofEpochMilli(200), false, participant1, new ItemPayload("id2", "value2")),
                new CatalogItemTest(Instant.ofEpochMilli(300), true, participant1, new ItemPayload("id3", "value3"))
        );
        foreignItems.forEach( item -> sharedCatalog.acceptForeignCatalogItem(item));
        sharedCatalog.acknowledgeReceivedItem(new CatalogItemTest(Instant.ofEpochMilli(300), true, participant1, new ItemPayload("id3", "value3")));
        Assertions.assertTrue(sharedCatalog.acknowledged());

        final AckReport<SubjectTest, ParticipantTest> ackReport = sharedCatalog.getAckReport();
        Assertions.assertEquals(1, ackReport.getItems().size());
        printReport(ackReport);
    }

    @Test
    public void testTwoParticipantsWrong() {
        sharedCatalog.start();
        final ParticipantTest participant1 = new ParticipantTest(UUID.randomUUID(), "participant1");
        final List<CatalogItem<SubjectTest, ParticipantTest>> foreignItems = Arrays.asList(
                new CatalogItemTest(Instant.ofEpochMilli(50), false, participant1, new ItemPayload("id1", "value1")),
                new CatalogItemTest(Instant.ofEpochMilli(200), false, participant1, new ItemPayload("id2", "value2")),
                new CatalogItemTest(Instant.ofEpochMilli(300), false, participant1, new ItemPayload("id3", "value3"))
        );
        foreignItems.forEach( item -> sharedCatalog.acceptForeignCatalogItem(item));
        sharedCatalog.acknowledgeReceivedItem(new CatalogItemTest(Instant.ofEpochMilli(300), true, participant1, new ItemPayload("id3", "value3")));
        Assertions.assertFalse(sharedCatalog.acknowledged());

        final AckReport<SubjectTest, ParticipantTest> ackReport = sharedCatalog.getAckReport();
        Assertions.assertEquals(1, ackReport.getItems().size());
        printReport(ackReport);
    }

    @Test
    public void testParallel() {
        sharedCatalog.start();

        final ParticipantTest participant1 = new ParticipantTest(UUID.randomUUID(), "participant1");
        final List<CatalogItem<SubjectTest, ParticipantTest>> foreignItems = Arrays.asList(
                new CatalogItemTest(Instant.ofEpochMilli(50), false, participant1, new ItemPayload("id1", "value1")),
                new CatalogItemTest(Instant.ofEpochMilli(200), false, participant1, new ItemPayload("id2", "value2")),
                new CatalogItemTest(Instant.ofEpochMilli(300), false, participant1, new ItemPayload("id3", "value3"))
        );
        sharedCatalog.acceptForeignCatalog(foreignItems);
        sharedCatalog.acknowledgeReceivedItem(new CatalogItemTest(Instant.ofEpochMilli(300), false, participant1, new ItemPayload("id3", "value3")));
        Assertions.assertTrue(sharedCatalog.acknowledged());

        final AckReport<SubjectTest, ParticipantTest> ackReport = sharedCatalog.getAckReport();
        Assertions.assertEquals(1, ackReport.getItems().size());
        printReport(ackReport);
    }

    private void printReport(AckReport<SubjectTest, ParticipantTest> ackReport) {
        LOGGER.debug("Report");
        ackReport.getItems().forEach((id, item) -> {
            LOGGER.debug(id + ": " + item);
        });
    }







}
