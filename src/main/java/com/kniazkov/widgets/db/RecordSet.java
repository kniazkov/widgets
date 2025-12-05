package com.kniazkov.widgets.db;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * An abstract base class representing a collection of records with filtering and sorting
 * capabilities.
 * <p>
 * Provides methods to retrieve, filter, and sort records. Implementations must ensure
 * that record collections are properly isolated from underlying storage changes.
 */
public abstract class RecordSet {
    /**
     * Returns the total number of records in this set.
     *
     * @return the record count
     */
    public abstract int getRecordCount();

    /**
     * Returns a snapshot of all records in this set.
     * <p>
     * The returned list is a copy that is isolated from subsequent modifications to the underlying
     * storage. Changes to the original record set after this method call will not affect
     * the returned list.
     *
     * @return a new list containing all records
     */
    public abstract List<Record> getRecords();


    /**
     * Returns all records sorted by creation time in chronological order
     * (oldest first).
     *
     * @return a list of records sorted from oldest to newest
     */
    public List<Record> getRecordsOldFirst() {
        List<Record> sortedRecords = getRecords();
        sortedRecords.sort(Comparator.comparing(Record::getTimestamp));
        return sortedRecords;
    }

    /**
     * Returns all records sorted by creation time in reverse chronological order
     * (newest first).
     *
     * @return a list of records sorted from newest to oldest
     */
    public List<Record> getRecordsNewFirst() {
        List<Record> sortedRecords = getRecords();
        sortedRecords.sort(Comparator.comparing(Record::getTimestamp).reversed());
        return sortedRecords;
    }

    /**
     * Creates a new record set containing only records that match the specified filter.
     * <p>
     * The returned record set is a live view that reflects the state of the original
     * set at the time of creation. Subsequent modifications to the original set
     * will not affect the filtered result.
     *
     * @param filter the filter criteria to apply
     * @return a new record set containing only matching records
     */
    public RecordSet select(final Filter filter) {
        final List<Record> filtered = new ArrayList<>();
        for (final Record record : this.getRecords()) {
            if (filter.match(record)) {
                filtered.add(record);
            }
        }
        return new RecordSet() {
            @Override
            public int getRecordCount() {
                return filtered.size();
            }

            @Override
            public List<Record> getRecords() {
                return new ArrayList<>(filtered);
            }
        };
    }
}
