package com.origami;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of a "forgetting map".
 * A forgetting map has a max capacity.
 * If an association is added on a full map the least "found" association will be removed and replaced.
 * If there are multiple least-used associations, the earliest association to be added from that group will be removed. See {@link CountAndTime}
 * This implementation is thread-safe.
 *
 * @param <K> Type of key
 * @param <V> Type of value
 */
public class ForgettingMap<K, V> {

    private final int capacity;
    private final Map<K, V> backingMap;
    private final Map<K, CountAndTime> associationCounterMap;

    /**
     * Constructs the map.
     *
     * @param capacity max entries allowed for the map.
     */
    public ForgettingMap(final int capacity) {
        assert capacity > 0;
        this.capacity = capacity;
        this.backingMap = new HashMap<>();
        this.associationCounterMap = new HashMap<>();
    }

    /**
     * Adds an association.
     * <p>
     * If the key doesn't already exist, and we are at max capacity, first remove the least used/found association.
     * <p>
     * Uses synchronized to make it thread-safe.
     *
     * @param key   to add.
     * @param value to add.
     */
    public synchronized void add(final K key, final V value) {
        if (!keyExists(key) && atCapacity()) {
            removeLeastFoundAssociation();
        }
        addAssociation(key, value);
    }

    /**
     * Removes an association.
     * <p>
     * Uses synchronized to make it thread-safe.
     *
     * @param key to remove.
     * @return the removed content, if it exists.
     */
    public synchronized V remove(final K key) {
        if (!keyExists(key)) {
            return null;
        } else {
            associationCounterMap.remove(key);
            return backingMap.remove(key);
        }
    }

    /**
     * Finds and returns the value for a given key, null if it doesn't exist in the map.
     * <p>
     * Uses synchronized to make it thread-safe.
     *
     * @param key to retrieve with.
     * @return the value if it exists, null otherwise.
     */
    public synchronized V find(final K key) {
        if (keyExists(key)) {
            associationCounterMap.get(key).incrementCount();
            return backingMap.get(key);
        } else {
            return null;
        }
    }

    /**
     * Removes the least used/found association.
     */
    private void removeLeastFoundAssociation() {
        K keyToRemove = findLeastFoundAssociation();
        backingMap.remove(keyToRemove);
        associationCounterMap.remove(keyToRemove);
    }

    /**
     * Finds the least used/found association by sorting on the value's natural order (See {@link CountAndTime},
     *
     * @return the key to remove
     */
    private K findLeastFoundAssociation() {
        return associationCounterMap
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toList())
                .get(0).getKey();
    }

    /**
     * @param key to check existence of.
     * @return true if exists.
     */
    private boolean keyExists(K key) {
        return backingMap.containsKey(key);
    }

    /**
     * Adds an association to the backingMap.
     * Adds a fresh counter for the key in associationCounterMap.
     *
     * @param key   to add.
     * @param value to add.
     */
    private void addAssociation(final K key, final V value) {
        backingMap.put(key, value);
        associationCounterMap.put(key, new CountAndTime());
    }

    /**
     * Checks if the map is at capacity.
     *
     * @return true if at capacity.
     */
    private boolean atCapacity() {
        return backingMap.size() == capacity;
    }

    /**
     * Returns an immutable view of the backingMap. Useful for testing purposes.
     *
     * @return the immutable backingMap
     */
    public Map<K, V> getImmutableBackingMap() {
        return Collections.unmodifiableMap(backingMap);
    }

    /**
     * Returns an immutable view of the associationCounterMap. Useful for testing purposes.
     *
     * @return the immutable associationCounterMap
     */
    public Map<K, CountAndTime> getImmutableAssociationCounterMap() {
        return Collections.unmodifiableMap(associationCounterMap);
    }

    /**
     * Internal class that is used to represent how often an association is used/found and when it was added in milliseconds.
     * Implements {@link Comparable} using the count as primary comparable field backed up by timeAddedInMilli field.
     */
    static class CountAndTime implements Comparable<CountAndTime> {

        private Integer count = 0;
        private final Long timeAddedInMilli = System.currentTimeMillis();

        @Override
        public int compareTo(CountAndTime o) {
            int toReturn;
            toReturn = this.count.compareTo(o.count);
            if (toReturn == 0) {
                toReturn = this.timeAddedInMilli.compareTo(o.timeAddedInMilli);
            }
            return toReturn;
        }

        void incrementCount() {
            count++;
        }

        public Integer getCount() {
            return count;
        }

    }
}
