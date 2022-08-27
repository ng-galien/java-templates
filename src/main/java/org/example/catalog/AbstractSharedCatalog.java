package org.example.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractSharedCatalog<K extends Subject, T> implements SharedCatalog<K, T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractSharedCatalog.class);

    protected final T owner;

    private final TemporalAmount quietPeriod;

    public AbstractSharedCatalog(final T owner, TemporalAmount quietPeriod) {
        this.owner = owner;
        this.quietPeriod = quietPeriod;
    }

    public void start() {
        LOGGER.info("Starting catalog");
        reset();
        setMyOwnList(fetchMyItems()).forEach(item -> {
                if (isTopicSupported(item) && !item.deleted()) {
                    addToSendList(item);
                }
            }
        );
    }

    private boolean isTopicSupported(CatalogItem<K, T> item) {
        if (!getAvailableTopics().contains(item.subject().topic())) {
            LOGGER.warn("Unsupported topic: {}", item.subject().topic());
            return false;
        }
        return true;
    }

    public void acceptForeignCatalogItem(CatalogItem<K, T> otherItem) {
        LOGGER.debug("Accepting foreign item {}", otherItem);
        if (!isTopicSupported(otherItem)) {
            return;
        }
        if(otherItem.owner().equals(owner)) {
            LOGGER.warn("The owners are identical: {}", owner);
            return;
        }
        Optional<CatalogItem<K, T>> fromMe = findInMyList(otherItem);
        Optional<CatalogItem<K, T>> fromExpected = fromExpectedList(otherItem);
        if (fromMe.isPresent()) {
            //State: item is in both catalogs
            CatalogItem<K, T> myItem = fromMe.get();
            if (fromExpected.isPresent()) {
                //State: items are in both catalogs and another participant has a newer version
                if (otherItem.isNewerThan(fromExpected.get(), quietPeriod)) {
                    //State: items are in both catalogs and item is expected and the current participant supplied a newer version
                    removeFromExpectedList(fromExpected.get());
                    addToExpectedList(otherItem);
                } else {
                    //State: items are in both catalogs and item is expected and the current participant supplied an older version
                    //Nothing to do
                }
            } else {
                //State: items are in both catalogs and no one has a newer version
                if (otherItem.isNewerThan(myItem, quietPeriod)) {
                    //State: items are in both catalogs and no one has a newer version and the current participant supplied a newer version
                    removeFromSendList(myItem);
                    addToExpectedList(otherItem);
                } else {
                    //State: items are in both catalogs and no one has a newer version and the current participant supplied an older version
                    //Nothing to do
                }
            }
        } else {
            //State: item is not in my catalog
            if (fromExpected.isPresent()) {
                //State: item is not in my catalog and item is expected
                if (otherItem.isNewerThan(fromExpected.get(), quietPeriod)) {
                    //State: item is not in my catalog and item is expected and the current participant supplied a newer version
                    removeFromExpectedList(fromExpected.get());
                    addToExpectedList(otherItem);
                } else {
                    //State: item is not in my catalog and item is expected and the current participant supplied an older version
                    //Nothing to do
                }
            } else {
                //State: item is not in my catalog and item is not expected
                if (otherItem.deleted()) {
                    //State: item is not in my catalog and item is not expected and the current participant supplied a deleted item
                    addToExpectedList(otherItem);
                } else {
                    //State: item is not in my catalog and item is not expected and the current participant supplied a new item
                    addToExpectedList(otherItem);
                }
            }
        }
    }

    @Override
    public void acceptForeignCatalog(Collection<CatalogItem<K, T>> otherItem) {
        LOGGER.info("Accepting foreign catalog");
        otherItem.parallelStream().forEach(this::acceptForeignCatalogItem);
    }

    @Override
    public void acknowledgeReceivedItem(CatalogItem<K, T> otherItem) {
        LOGGER.trace("Acknowledging received item {}", otherItem);
        fromExpectedList(otherItem).ifPresent(found -> {
                if (found.isNewerThan(otherItem, quietPeriod)) {
                    LOGGER.warn("Received item is older than expected item: {}, difference is {}", otherItem,
                            Duration.between(otherItem.date(), found.date()));
                    return;
                }
                if (found.deleted() != otherItem.deleted()) {
                    LOGGER.warn("Received item is not deleted but expected item is deleted: {}", otherItem);
                    return;
                }
                if (!found.owner().equals(otherItem.owner())) {
                    LOGGER.warn("Received item has different owner than expected item: {}", otherItem);
                    return;
                }
                removeFromExpectedList(found).ifPresent(item -> {
                        final AckItemRecord<T> ackItem = new AckItemRecord<>(true,
                                otherItem.deleted(),
                                item.owner(), item.date());
                        if (saveAckStatus(otherItem.subject(), ackItem)) {
                            onAcknowledged(otherItem.subject(), ackItem);
                        } else {
                            LOGGER.warn("Could not save ack status for {}", otherItem.subject());
                        }
                    }
                );
            }
        );
    }

    @Override
    public boolean acknowledged() {
        return fetchExpectedList().isEmpty();
    }

    @Override
    public AckReport<K, T> getAckReport() {
        final Map<K, AckItem<T>> ackStatus = fetchAckState();
        final Collection<CatalogItem<K, T>> expected = fetchExpectedList();
        expected.forEach(item -> ackStatus.put(item.subject(), new AckItemRecord<>(
                false,
                item.deleted(),
                item.owner(),
                item.date())
        ));
        return new AckReportRecord<>(expected.isEmpty(), ackStatus);
    }

    protected abstract void reset();

    protected abstract Collection<CatalogItem<K, T>> setMyOwnList(Collection<CatalogItem<K, T>> items);

    protected abstract Optional<CatalogItem<K, T>> findInMyList(CatalogItem<K, T> otherItem);

    protected abstract void addToSendList(CatalogItem<K, T> item);

    protected abstract void removeFromSendList(CatalogItem<K, T> item);

    protected abstract Optional<CatalogItem<K, T>> removeFromExpectedList(CatalogItem<K, T> item);

    protected abstract void addToExpectedList(CatalogItem<K, T> item);

    protected abstract Optional<CatalogItem<K, T>> fromExpectedList(CatalogItem<K, T> item);

    protected abstract boolean saveAckStatus(K id, AckItem<T> status);

    protected abstract Map<K, AckItem<T>> fetchAckState();

    protected abstract Collection<CatalogItem<K, T>> fetchExpectedList();

    protected abstract Collection<CatalogItem<K, T>> fetchMyItems();

    protected abstract Collection<String> getAvailableTopics();

}
