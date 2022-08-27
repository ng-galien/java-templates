package org.example.catalog;

import java.util.Collection;
import java.util.List;

public interface SharedCatalog<K extends Subject, T> {

    void start();

    void acceptForeignCatalogItem(CatalogItem<K, T> otherItem);

    void acceptForeignCatalog(List<CatalogItem<K, T>> otherItem);

    void acknowledgeReceivedItem(CatalogItem<K, T>  otherItem);

    Collection<CatalogItem<K, T>> getItemsToShare();

    void onAcknowledged(K key, AckItem<T> ackItem);

    AckReport<K, T> getAckReport();

    boolean acknowledged();

}
