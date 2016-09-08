package uk.gov.justice.services.core.aggregate;

import static java.lang.String.format;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.extension.EventFoundEvent;
import uk.gov.justice.services.eventsourcing.repository.core.exception.DuplicateSnapshotException;
import uk.gov.justice.services.eventsourcing.repository.core.exception.InvalidSequenceIdException;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.snapshot.SnapshotService;
import uk.gov.justice.services.eventsourcing.source.core.snapshot.VersionedAggregate;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Service for replaying event streams on aggregates.
 */
@ApplicationScoped
public class AggregateService {

    @Inject
    Logger logger;

    @Inject
    SnapshotService snapshotService;

    @Inject
    JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private ConcurrentHashMap<String, Class<?>> eventMap = new ConcurrentHashMap<>();

    /**
     * Recreate an aggregate of the specified type by replaying the events from an event stream.
     *
     * @param stream the event stream to replay
     * @param clazz  the type of aggregate to recreate
     * @return the recreated aggregate
     */
    public <T extends Aggregate>  T get(final EventStream stream,
                 final Class<T> clazz)
            throws DuplicateSnapshotException, InvalidSequenceIdException {

        logger.trace("Recreating aggregate for instance {} of aggregate type {}", stream.getId(), clazz);

        VersionedAggregate<T> versionedAggregate = aggregateOf(stream, clazz);
        final Long snapshotVersion = versionedAggregate.getVersionId();

        T newAggregate = applyEvents(stream, versionedAggregate.getAggregate(), snapshotVersion);

        snapshotService.attemptAggregateStore(stream.getId(), stream.getCurrentVersion(), clazz, newAggregate, snapshotVersion);

        return newAggregate;
    }

    private <T extends Aggregate>  T applyEvents(final EventStream stream,
                          final T aggregate,
                          final long recentVersionId) {
        logger.trace("Apply Events for {}", stream, recentVersionId);

        Stream<JsonEnvelope> filteredEvent = stream.readFrom(recentVersionId);
        aggregate.apply(filteredEvent.map(this::convertEnvelopeToEvent));
        return aggregate;
    }

    private <T extends Aggregate>  VersionedAggregate<T> aggregateOf(final EventStream stream, final Class<T> clazz) {
        return snapshotService.getLatestVersionedAggregate(stream.getId(), clazz);
    }


    /**
     * Register method, invoked automatically to register all event classes into the eventMap.
     *
     * @param event identified by the framework to be registered into the event map
     */
    void register(@Observes final EventFoundEvent event) {
        logger.info("Registering event {}, {} with AggregateService", event.getEventName(), event.getClazz());
        eventMap.putIfAbsent(event.getEventName(), event.getClazz());
    }

    private Object convertEnvelopeToEvent(final JsonEnvelope event) {
        final String name = event.metadata().name();
        if (!eventMap.containsKey(name)) {
            throw new IllegalStateException(format("No event class registered for events of type %s", name));
        }
        return jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), eventMap.get(name));
    }
}
