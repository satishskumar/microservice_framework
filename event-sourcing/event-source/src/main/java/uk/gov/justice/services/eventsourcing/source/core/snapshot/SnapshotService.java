package uk.gov.justice.services.eventsourcing.source.core.snapshot;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.domain.snapshot.AggregateChangeDetectedException;
import uk.gov.justice.domain.snapshot.ObjectInputStreamStrategy;

import java.util.UUID;


/**
 * The interface Snapshot service.
 */
public interface SnapshotService {

    /**
     * Stores an aggregate .
     *
     * @param <T>                    the type parameter
     * @param streamId               the stream id
     * @param streamVersionId        the stream version id
     * @param clazz                  the clazz
     * @param aggregate              the aggregate
     * @param currentSnapshotVersion the current snapshot version
     */
    public <T extends Aggregate> void attemptAggregateStore(final UUID streamId,
                                                            final long streamVersionId,
                                                            final Class<T> clazz,
                                                            final T aggregate,
                                                            final long currentSnapshotVersion);

    /**
     * Get an Versioned Aggregate.
     *
     * @param <T>            the type parameter
     * @param streamId       the id of the stream to retrieve
     * @param clazz          the clazz
     * @param streamStrategy the stream strategy
     * @return the VersionedAggregate. Never returns null.
     * @throws AggregateChangeDetectedException the aggregate change detected exception
     */
    public <T extends Aggregate> VersionedAggregate<T> getLatestVersionedAggregate(final UUID streamId,
                                                                                   final Class<T> clazz,
                                                                                   final ObjectInputStreamStrategy streamStrategy)
            throws AggregateChangeDetectedException;

    /**
     * deletes all snapshots.
     *
     * @param <T>      the type parameter
     * @param streamId the id of the stream to retrieve
     * @param clazz    of the aggregate to retrieve
     */
    public <T extends Aggregate> void removeAllSnapshots(UUID streamId, Class<T> clazz);

    /**
     * Get an Versioned Aggregate.
     *
     * @param <T>   the type parameter
     * @param clazz of the aggregate to retrieve
     * @return the VersionedAggregate. Never returns null.
     */
    public <T extends Aggregate> VersionedAggregate<T> getNewVersionedAggregate(final Class<T> clazz);
}
