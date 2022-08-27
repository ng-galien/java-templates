package org.example.catalog.test;

import org.example.catalog.*;
import org.example.catalog.test.mock.ItemInfo;
import org.example.catalog.test.mock.ItemPayload;
import org.example.catalog.test.mock.ParticipantTest;
import org.example.catalog.test.mock.SubjectTest;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;


public class SharedCatalogTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(SharedCatalogTest.class);

    private SharedCatalog<Subject, ParticipantTest> sharedCatalog;

    private final ParticipantTest owner = new ParticipantTest(UUID.randomUUID(), "owner");

    private final ParticipantTest otherParticipant = new ParticipantTest(UUID.randomUUID(), "other");

    public static final String TEST_TOPIC = "testTopic";
    private static final String TEST_SEPARATOR = "=========================== {} ===============================";

    private final TemporalAmount quietPeriod = Duration.ofMillis(0);

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
        sharedCatalog = new DefaultSharedCatalog<>(owner, quietPeriod) {

            {
                topics.put(TEST_TOPIC, true);
            }

            @Override
            public void onAcknowledged(Subject topic, AckItem<ParticipantTest> ackItem) {
                LOGGER.warn("onAcknowledged: topic={}, ackItem={}", topic, ackItem);
            }

            @Override
            protected Collection<CatalogItem<Subject, ParticipantTest>> fetchMyItems() {
                return getOwnItems();
            }
        };
    }

    @Test
    public void testItemIsSame() {
        CatalogItem<Subject, ParticipantTest> item1 = createItem(new ItemInfo(100, false, "item1"), owner);
        CatalogItem<Subject, ParticipantTest> item2 = createItem(new ItemInfo(100, false, "item1"), owner);
        Assertions.assertFalse(item2.isNewerThan(item1, Duration.ofMillis(0)));
    }

    @Test
    public void testItemIsNewerThan() {
        CatalogItem<Subject, ParticipantTest> item1 = createItem(new ItemInfo(100, false, "item1"), owner);
        CatalogItem<Subject, ParticipantTest> item2 = createItem(new ItemInfo(200, false, "item1"), owner);
        Assertions.assertTrue(item2.isNewerThan(item1, Duration.ofMillis(0)));
    }

    @Test
    public void testItemIsNotNewerThan() {
        CatalogItem<Subject, ParticipantTest> item1 = createItem(new ItemInfo(100, false, "item1"), owner);
        CatalogItem<Subject, ParticipantTest> item2 = createItem(new ItemInfo(200, false, "item1"), owner);
        Assertions.assertFalse(item1.isNewerThan(item2, Duration.ofMillis(0)));
    }

    private Collection<CatalogItem<Subject, ParticipantTest>> getOwnItems() {
        return createOtherItemList(new ItemInfo[]{
                new ItemInfo(50, false, "item1"),
                new ItemInfo(100, false, "item2"),
                new ItemInfo(200, false, "item3"),
        }, owner);
    }

    @Test
    public void testTwoParticipantUpdated() {
        sharedCatalog.start();
        final Collection<CatalogItem<Subject, ParticipantTest>> foreignItems =
                createOtherItemList(new ItemInfo[]{
                        new ItemInfo(50, false, "item1"),
                        new ItemInfo(300, false, "item2"),
                        new ItemInfo(300, true, "item3"),
                });
        sharedCatalog.acceptForeignCatalog(foreignItems);
        sharedCatalog.acknowledgeReceivedItem(createItem(new ItemInfo(300, false, "item2"), otherParticipant));
        sharedCatalog.acknowledgeReceivedItem(createItem(new ItemInfo(300, true, "item3"), otherParticipant));
        Assertions.assertTrue(sharedCatalog.acknowledged());
        Assertions.assertEquals(1, sharedCatalog.getItemsToShare().size());

        final AckReport<Subject, ParticipantTest> ackReport = sharedCatalog.getAckReport();
        Assertions.assertEquals(2, ackReport.items().size());
        printReport(ackReport);
    }

    @Test
    public void testTwoParticipantNewer() {
        sharedCatalog.start();
        final Collection<CatalogItem<Subject, ParticipantTest>> foreignItems =
                createOtherItemList(new ItemInfo[]{
                        new ItemInfo(50, false, "item1"),
                        new ItemInfo(300, false, "item2"),
                        new ItemInfo(100, true, "item3"),
                });
        sharedCatalog.acceptForeignCatalog(foreignItems);
        sharedCatalog.acknowledgeReceivedItem(createItem(new ItemInfo(300, false, "item2"), otherParticipant));
        Assertions.assertTrue(sharedCatalog.acknowledged());
        Assertions.assertEquals(2, sharedCatalog.getItemsToShare().size());

        final AckReport<Subject, ParticipantTest> ackReport = sharedCatalog.getAckReport();
        Assertions.assertEquals(1, ackReport.items().size());
        printReport(ackReport);
    }

    @Test
    public void testTwoParticipantDelete() {
        sharedCatalog.start();
        final Collection<CatalogItem<Subject, ParticipantTest>> foreignItems =
                createOtherItemList(new ItemInfo[]{
                        new ItemInfo(50, false, "item1"),
                        new ItemInfo(100, false, "item2"),
                        new ItemInfo(300, true, "item3"),
                });
        sharedCatalog.acceptForeignCatalog(foreignItems);
        sharedCatalog.acknowledgeReceivedItem(createItem(300, true, "item3", otherParticipant));
        Assertions.assertTrue(sharedCatalog.acknowledged());

        final AckReport<Subject, ParticipantTest> ackReport = sharedCatalog.getAckReport();
        Assertions.assertEquals(1, ackReport.items().size());
        printReport(ackReport);
    }

    @Test
    public void testTwoParticipantsWrong() {
        sharedCatalog.start();
        final ParticipantTest participant1 = new ParticipantTest(UUID.randomUUID(), "participant1");
        final Collection<CatalogItem<Subject, ParticipantTest>> foreignItems = createOtherItemList(new ItemInfo[]{
                new ItemInfo(10, false, "item1"),
                new ItemInfo(20, false, "item2"),
                new ItemInfo(300, false, "item3"),
        });
        sharedCatalog.acceptForeignCatalog(foreignItems);
        sharedCatalog.acknowledgeReceivedItem(createItem(300, false, "item3", participant1));
        Assertions.assertFalse(sharedCatalog.acknowledged());

        final AckReport<Subject, ParticipantTest> ackReport = sharedCatalog.getAckReport();
        Assertions.assertEquals(1, ackReport.items().size());
        printReport(ackReport);
    }


    @Test
    public void testParallel() {
        sharedCatalog.start();
        final Collection<CatalogItem<Subject, ParticipantTest>> foreignItems = createOtherItemList(new ItemInfo[]{
                        new ItemInfo(50, false, "item2"),
                        new ItemInfo(300, false, "item3"),
                });
        sharedCatalog.acceptForeignCatalog(foreignItems);
        sharedCatalog.acknowledgeReceivedItem(createItem(300, false, "item3", otherParticipant));
        Assertions.assertTrue(sharedCatalog.acknowledged());
        final AckReport<Subject, ParticipantTest> ackReport = sharedCatalog.getAckReport();
        Assertions.assertEquals(1, ackReport.items().size());
        printReport(ackReport);
    }

    private void printReport(AckReport<Subject, ParticipantTest> ackReport) {
        LOGGER.debug("Report");
        ackReport.items().forEach((id, item) -> {
            LOGGER.debug(id + ": " + item);
        });
    }


    private CatalogItem<Subject, ParticipantTest> createItem(
            long t, boolean deleted,  final String id, ParticipantTest participant) {
        return new CatalogItemRecord<>(Instant.ofEpochMilli(t), deleted, new SubjectTest(TEST_TOPIC, id), participant,
                new ItemPayload(id, "value"));
    }

    private CatalogItem<Subject, ParticipantTest> createItem(
            ItemInfo info, ParticipantTest participant) {
        return createItem(info.time(), info.deleted(), info.id(), participant);
    }

    private Collection<CatalogItem<Subject, ParticipantTest>> createOtherItemList(
            ItemInfo[] infos, ParticipantTest participant) {
        return Arrays.stream(infos).sequential().map(
                info -> createItem(info, participant))
                .collect(Collectors.toList());
    }

    private Collection<CatalogItem<Subject, ParticipantTest>> createOtherItemList(
            ItemInfo[] infos) {
        return Arrays.stream(infos).sequential().map(
                        info -> createItem(info, otherParticipant))
                .collect(Collectors.toList());
    }

}
