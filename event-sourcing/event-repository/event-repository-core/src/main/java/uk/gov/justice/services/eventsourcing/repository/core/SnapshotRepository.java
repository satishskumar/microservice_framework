package uk.gov.justice.services.eventsourcing.repository.core;


import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.domain.snapshot.AggregateSnapshot;

import java.util.Optional;
import java.util.UUID;

/**
 * Service to store and read snapshots.
 */
public interface SnapshotRepository {

    /**
     * Stores an aggregate snapshot.
     *
     * @param AggregateSnapshot aggregateSnapshot
     */
    void storeSnapshot(final AggregateSnapshot AggregateSnapshot);

    /**
     * Get an Optional Latest Aggregate Snapshot.
     *
     * @param streamId the id of the stream to retrieve
     * @return the Optional<AggregateSnapshot>. Never returns null.
     */
    Optional<AggregateSnapshot> getLatestSnapshot(final UUID streamId);


    /**
     * deletes all snapshots.
     *
     * @param <T>      the type parameter
     * @param streamId the id of the stream to retrieve
     * @param clazz    the clazz
     */
    <T extends Aggregate> void removeAllSnapshots(UUID streamId, Class<T> clazz);
}
