package uk.gov.justice.services.eventsourcing.source.core.snapshot;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.domain.snapshot.AggregateSnapshot;
import uk.gov.justice.services.eventsourcing.repository.core.SnapshotRepository;
import uk.gov.justice.services.eventsourcing.repository.core.exception.DuplicateSnapshotException;
import uk.gov.justice.services.eventsourcing.repository.core.exception.InvalidSequenceIdException;
import uk.gov.justice.services.eventsourcing.source.core.exception.AggregateCreationException;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang.SerializationException;
import org.slf4j.Logger;

@ApplicationScoped
public class DefaultSnapshotService implements SnapshotService {

    private static final long INITIAL_AGGREGATE_VERSION = 0;

    @Inject
    Logger logger;

    @Inject
    SnapshotRepository snapshotRepository;


    @Inject
    SnapshotStrategy snapshotStrategy;

    @Override
    public <T extends Aggregate> void attemptAggregateStore(final UUID streamId,
                                                            final long aggregateVersionId,
                                                            final Class<T> clazz,
                                                            final T aggregate,
                                                            final long snapshotVersionId)
            throws DuplicateSnapshotException, InvalidSequenceIdException {
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
                                                                                final Class<T> clazz) {

        logger.trace("Retrieving aggregate container strategy for {}", streamId, clazz);

        Optional<AggregateSnapshot> aggregateSnapshot = snapshotRepository.getLatestSnapshot(streamId);

        return versionedAggregateOf(aggregateSnapshot, clazz);
    }

    private <A extends Aggregate> VersionedAggregate versionedAggregateOf(
            final Optional<AggregateSnapshot> aggregateSnapshot,
            final Class<A> clazz) {
        logger.trace("Retrieving aggregate container for {}", aggregateSnapshot, clazz);
        try {

            return aggregateSnapshot.map(snapshot ->
                    new VersionedAggregate(
                            snapshot.getVersionId(),
                            snapshot.getAggregate())
            ).orElse(new VersionedAggregate(INITIAL_AGGREGATE_VERSION, clazz.newInstance()));

        } catch (SerializationException e) {
            logger.error("SerializationException for {}", aggregateSnapshot, clazz, e.getLocalizedMessage());
            return versionedAggregateOf(snapshotRepository.getEarlierSnapshot(aggregateSnapshot.get().getStreamId(),
                    aggregateSnapshot.get().getVersionId()), clazz);
        } catch (InstantiationException ex) {
            throw new AggregateCreationException(ex);
        } catch (IllegalAccessException e) {
            throw new AggregateCreationException(e);
        }

    }

}

