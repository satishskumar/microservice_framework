package uk.gov.justice.services.eventsourcing.source.core.snapshot;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.eventsourcing.repository.core.exception.DuplicateSnapshotException;
import uk.gov.justice.services.eventsourcing.repository.core.exception.InvalidSequenceIdException;

import java.util.UUID;


public interface SnapshotService {

    /**
     * Stores an aggregate .
     */
    public <T extends Aggregate> void attemptAggregateStore(final UUID streamId, final long streamVersionId, final Class<T> clazz, final T aggregate, final long currentSnapshotVersion)
            throws DuplicateSnapshotException, InvalidSequenceIdException;

    /**
     * Get an Versioned Aggregate.
     *
     * @param streamId the id of the stream to retrieve
     * @return the Optional<AggregateSnapshot>. Never returns null.
     */
    public <T extends Aggregate> VersionedAggregate<T> getLatestVersionedAggregate(final UUID streamId, final Class<T> clazz) ;

}
