package io.smallrye.context;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * This lookup map is optimized for the case where there is a single writer and multiple readers.
 * <p>
 * This map is not thread-safe for multiple writers and is not optimized for many entries, given
 * that is backed by an ordered array and uses linear search.
 */
final class SingleWriterCopyOnWriteArrayIdentityMap<K, V> {

    private static final AtomicReferenceFieldUpdater<SingleWriterCopyOnWriteArrayIdentityMap, Object[]> ENTRIES_UPDATER = AtomicReferenceFieldUpdater
            .newUpdater(SingleWriterCopyOnWriteArrayIdentityMap.class, Object[].class, "entries");

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private volatile Object[] entries;

    public SingleWriterCopyOnWriteArrayIdentityMap() {
        ENTRIES_UPDATER.lazySet(this, EMPTY_ARRAY);
    }

    public V get(K key) {
        final Object[] array = this.entries;
        for (int i = 0; i < array.length; i += 2) {
            if (array[i] == key) {
                return (V) array[i + 1];
            }
        }
        return null;
    }

    public void put(K key, V value) {
        Object[] oldEntries = entries;
        // verify if the key already exists in the array
        // or if the value is the same
        int keyIndex = -1;
        for (int i = 0; i < oldEntries.length; i += 2) {
            if (oldEntries[i] == key) {
                if (oldEntries[i + 1] == value) {
                    return;
                }
                keyIndex = i;
                break;
            }
        }
        final Object[] newEntries;
        if (keyIndex != -1) {
            // key already exists, but the value is different
            newEntries = new Object[oldEntries.length];
            System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
            newEntries[keyIndex + 1] = value;
        } else {
            // key does not exist, add it
            newEntries = new Object[oldEntries.length + 2];
            System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
            newEntries[oldEntries.length] = key;
            newEntries[oldEntries.length + 1] = value;
        }
        ENTRIES_UPDATER.lazySet(this, newEntries);
    }

    public void removeEntriesWithValue(V value) {
        final Object[] oldEntries = entries;
        // verify first where the first value is found
        int firstKeyWithValueMatches = -1;
        for (int i = 0; i < oldEntries.length; i += 2) {
            if (oldEntries[i + 1] == value) {
                firstKeyWithValueMatches = i;
                break;
            }
        }
        if (firstKeyWithValueMatches == -1) {
            // value not found
            return;
        }
        // create a new ArrayList of survivors (we're generous with the initial size)
        Object[] newEntries = new Object[oldEntries.length - 2];
        // copy the first part of the array till the matching key/value pair
        if (firstKeyWithValueMatches > 0) {
            System.arraycopy(oldEntries, 0, newEntries, 0, firstKeyWithValueMatches);
        }
        int newIdx = firstKeyWithValueMatches;
        // filter and add the other key/value pairs, not matching the value
        for (int i = firstKeyWithValueMatches + 2; i < oldEntries.length; i += 2) {
            if (oldEntries[i + 1] != value) {
                newEntries[newIdx] = oldEntries[i];
                newEntries[newIdx + 1] = oldEntries[i + 1];
                newIdx += 2;
            }
        }
        // create a new exact array if necessary
        if (newIdx < newEntries.length) {
            newEntries = newIdx == 0 ? EMPTY_ARRAY : Arrays.copyOf(newEntries, newIdx);
        }
        ENTRIES_UPDATER.lazySet(this, newEntries);
    }

}
