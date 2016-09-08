package uk.gov.justice.services.eventsourcing.repository.core;


import uk.gov.justice.domain.snapshot.AggregateSnapshot;
import uk.gov.justice.services.eventsourcing.repository.core.exception.DuplicateSnapshotException;
import uk.gov.justice.services.eventsourcing.repository.core.exception.InvalidSequenceIdException;

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
    void storeSnapshot(final AggregateSnapshot AggregateSnapshot)
            throws DuplicateSnapshotException, InvalidSequenceIdException;

    /**
     * Get an Optional Aggregate Snapshot.
     *
     * @param streamId the id of the stream to retrieve
     * @return the Optional<AggregateSnapshot>. Never returns null.
     */
    Optional<AggregateSnapshot> getLatestSnapshot(final UUID streamId);


    /**
     * Get an Optional Aggregate Snapshot.
     *
     * @param streamId the id of the stream to retrieve
     * @return the Optional<AggregateSnapshot>. Never returns null.
     */
    Optional<AggregateSnapshot> getEarlierSnapshot(final UUID streamId, final long versionId);
}
