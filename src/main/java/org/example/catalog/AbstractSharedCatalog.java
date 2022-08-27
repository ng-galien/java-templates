package org.example.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractSharedCatalog<K extends Subject, T> implements SharedCatalog<K, T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractSharedCatalog.class);

    protected final T owner;

    public AbstractSharedCatalog(final T owner) {
        this.owner = owner;
    }

    public void start() {
        LOGGER.debug("Starting catalog");
        setMyOwnList(fetchMyItems()).forEach(item -> {
                if (isTopicSupported(item) && !item.isDeleted()) {
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
        Optional<CatalogItem<K, T>> found = findInMyList(otherItem);
        if (found.isPresent()) {
            CatalogItem<K, T> myItem = found.get();
            if (otherItem.isNewerThan(myItem)) {
                removeFromSendList(myItem);
                addToNewerList(otherItem);
                addToExpectedList(otherItem);
            } else {
                if (!otherItem.isDeleted()) {
                    removeFromSendList(myItem);
                } else {
                    if(!existsInNewerList(otherItem)) {
                        addToSendList(myItem);
                    }
                }
            }
        } else {
            if (!otherItem.isDeleted()) {
                addToExpectedList(otherItem);
            }
        }
    }

    @Override
    public void acceptForeignCatalog(List<CatalogItem<K, T>> otherItem) {
        otherItem.parallelStream().forEach(this::acceptForeignCatalogItem);
    }

    @Override
    public void acknowledgeReceivedItem(CatalogItem<K, T> otherItem) {
        LOGGER.trace("Acknowledging received item {}", otherItem);
        fromExpectedList(otherItem).ifPresent(found -> {
            if (found.isDeleted() == otherItem.isDeleted()) {
                    removeFromExpectedList(found).ifPresent(item -> {
                            final DefaultAckItem<T> ackItem = new DefaultAckItem<>(true,
                                    otherItem.isDeleted(),
                                    item.owner(), item.getTimestamp());
                            saveAckStatus(otherItem.subject(), ackItem);
                            onAcknowledged(otherItem.subject(), ackItem);
                        }
                    );
                }
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
        expected.forEach(item -> ackStatus.put(item.subject(), new DefaultAckItem<>(
                false,
                item.isDeleted(),
                item.owner(),
                item.getTimestamp())
        ));
        return new DefaultAckReport<>(expected.isEmpty(), ackStatus);
    }

    protected abstract Collection<CatalogItem<K, T>> setMyOwnList(Collection<CatalogItem<K, T>> items);

    protected abstract Optional<CatalogItem<K, T>> findInMyList(CatalogItem<K, T> otherItem);

    protected abstract void addToSendList(CatalogItem<K, T> item);

    protected abstract void addToNewerList(CatalogItem<K, T> item);

    protected abstract boolean existsInNewerList(CatalogItem<K, T> item);

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
