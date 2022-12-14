package org.example.catalog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DefaultSharedCatalog<K extends Subject, T> extends AbstractSharedCatalog<K, T> {

    public DefaultSharedCatalog(T owner) {
        super(owner);
    }

    @Override
    public Optional<CatalogItem<K, T>> findInMyList(final CatalogItem<K, T> otherItem) {
        return Optional.ofNullable(ownList.get(otherItem.subject()));
    }

    @Override
    protected void addToNewerList(CatalogItem<K, T> item) {
        newerList.put(item.subject(), item);
    }

    @Override
    protected boolean existsInNewerList(CatalogItem<K, T> item) {
        return newerList.containsKey(item.subject());
    }

    @Override
    public void addToSendList(final CatalogItem<K, T> item) {
        sendList.put(item.subject(), item);
    }

    @Override
    public void removeFromSendList(final CatalogItem<K, T> item) {
        sendList.remove(item.subject());
    }

    @Override
    public void addToExpectedList(final CatalogItem<K, T> item) {
        expectedList.put(item.subject(), item);
    }

    @Override
    protected Optional<CatalogItem<K, T>> removeFromExpectedList(CatalogItem<K, T> item) {
        return Optional.ofNullable(expectedList.remove(item.subject()));
    }

    @Override
    public Collection<CatalogItem<K, T>> getItemsToShare() {
        return sendList.values();
    }

    @Override
    protected boolean saveAckStatus(K id, AckItem<T> status) {
        return ackList.put(id, status) == null;
    }

    @Override
    protected Map<K, AckItem<T>> fetchAckState() {
        return new HashMap<>(ackList);
    }

    @Override
    protected Collection<CatalogItem<K, T>> fetchExpectedList() {
        return expectedList.values();
    }

    @Override
    protected Optional<CatalogItem<K, T>> fromExpectedList(CatalogItem<K, T> item) {
        return Optional.ofNullable(expectedList.get(item.subject()));
    }

    @Override
    protected Collection<CatalogItem<K, T>> setMyOwnList(Collection<CatalogItem<K, T>> catalogItems) {
        ownList.clear();
        catalogItems.forEach(item -> {
            ownList.put(item.subject(), item);
        });
        return ownList.values();
    }

    @Override
    protected Collection<String> getAvailableTopics() {
        return topics.keySet();
    }

    private final Map<K, CatalogItem<K, T>> ownList = new ConcurrentHashMap<>();
    private final Map<K, CatalogItem<K, T>> sendList = new ConcurrentHashMap<>();
    private final Map<K, CatalogItem<K, T>> expectedList = new ConcurrentHashMap<>();

    private final Map<K, CatalogItem<K, T>> newerList = new ConcurrentHashMap<>();

    private final Map<K, AckItem<T>> ackList = new ConcurrentHashMap<>();

    protected final Map<String, Boolean> topics = new ConcurrentHashMap<>();

}
