package uk.gov.justice.services.eventsourcing.source.core.snapshot;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.domain.snapshot.AggregateChangeDetectedException;
import uk.gov.justice.domain.snapshot.AggregateSnapshot;
import uk.gov.justice.domain.snapshot.ObjectInputStreamStrategy;
import uk.gov.justice.services.eventsourcing.repository.core.SnapshotRepository;
import uk.gov.justice.services.eventsourcing.source.core.exception.AggregateCreationException;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang.SerializationException;
import org.slf4j.Logger;

/**
 * The type Default snapshot service.
 */
@ApplicationScoped
public class DefaultSnapshotService implements SnapshotService {

    private static final long INITIAL_AGGREGATE_VERSION = 0;

    /**
     * The Logger.
     */
    @Inject
    Logger logger;

    /**
     * The Snapshot repository.
     */
    @Inject
    SnapshotRepository snapshotRepository;


    /**
     * The Snapshot strategy.
     */
    @Inject
    SnapshotStrategy snapshotStrategy;

    @Override
    public <T extends Aggregate> void attemptAggregateStore(final UUID streamId,
                                                            final long aggregateVersionId,
                                                            final Class<T> clazz,
                                                            final T aggregate,
                                                            final long snapshotVersionId) {
        try {
            logger.trace("Applying snapshot Strategy for {}", streamId, aggregateVersionId, clazz, aggregate, snapshotVersionId);
            if (snapshotStrategy.shouldCreateSnapshot(aggregateVersionId, snapshotVersionId)) {
                snapshotRepository.storeSnapshot(new AggregateSnapshot(streamId,
                        aggregateVersionId, clazz, aggregate));
            }
        } catch (SerializationException e) {
            logger.error("SerializationException while creating snapshot Strategy for {}", streamId, aggregateVersionId, clazz, aggregate, snapshotVersionId);
        }
    }

    @Override
    public <T extends Aggregate> VersionedAggregate getLatestVersionedAggregate(final UUID streamId,
                                                                                final Class<T> clazz,
                                                                                final ObjectInputStreamStrategy streamStrategy)
            throws AggregateChangeDetectedException {

        logger.trace("Retrieving aggregate container strategy for {}", streamId, clazz);

        Optional<AggregateSnapshot> aggregateSnapshot = snapshotRepository.getLatestSnapshot(streamId);

        return versionedAggregateOf(aggregateSnapshot, clazz, streamStrategy);
    }

    @Override
    public <T extends Aggregate> void removeAllSnapshots(UUID streamId, Class<T> clazz) {
        logger.trace("Removing all snapshots for {}", streamId, clazz);
        snapshotRepository.removeAllSnapshots(streamId, clazz);

    }

    @Override
    public <T extends Aggregate> VersionedAggregate<T> getNewVersionedAggregate(final Class<T> clazz)
            throws AggregateCreationException {
        try {
            return new VersionedAggregate(INITIAL_AGGREGATE_VERSION, clazz.newInstance());
        } catch (InstantiationException ex) {
            throw new AggregateCreationException(ex);
        } catch (IllegalAccessException e) {
            throw new AggregateCreationException(e);
        }
    }

    private <A extends Aggregate> VersionedAggregate versionedAggregateOf(
            final Optional<AggregateSnapshot> aggregateSnapshot,
            final Class<A> clazz,
            final ObjectInputStreamStrategy streamStrategy) throws AggregateChangeDetectedException {
        logger.trace("Retrieving aggregate container for {}", aggregateSnapshot, clazz);
        if (aggregateSnapshot.isPresent()) {
            return new VersionedAggregate(
                    aggregateSnapshot.get().getVersionId(),
                    aggregateSnapshot.get().getAggregate(streamStrategy));
        } else {
            return getNewVersionedAggregate(clazz);
        }
    }
}

