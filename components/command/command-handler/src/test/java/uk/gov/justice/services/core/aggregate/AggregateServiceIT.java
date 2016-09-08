package uk.gov.justice.services.core.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelope;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.domain.snapshot.AggregateSnapshot;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.cdi.LoggerProducer;
import uk.gov.justice.services.core.extension.EventFoundEvent;
import uk.gov.justice.services.eventsource.DefaultEventDestinationResolver;
import uk.gov.justice.services.eventsourcing.publisher.jms.JmsEventPublisher;
import uk.gov.justice.services.eventsourcing.repository.core.EventRepository;
import uk.gov.justice.services.eventsourcing.repository.core.exception.DuplicateSnapshotException;
import uk.gov.justice.services.eventsourcing.repository.core.exception.InvalidSequenceIdException;
import uk.gov.justice.services.eventsourcing.repository.jdbc.JdbcEventRepository;
import uk.gov.justice.services.eventsourcing.repository.jdbc.eventlog.EventLogConverter;
import uk.gov.justice.services.eventsourcing.source.core.EnvelopeEventStream;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.EventStreamManager;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.eventsourcing.source.core.snapshot.DefaultSnapshotService;
import uk.gov.justice.services.eventsourcing.source.core.snapshot.DefaultSnapshotStrategy;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.jms.EnvelopeConverter;
import uk.gov.justice.services.messaging.jms.JmsEnvelopeSender;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.openejb.jee.WebApp;
import org.apache.openejb.junit.ApplicationComposer;
import org.apache.openejb.testing.Application;
import org.apache.openejb.testing.Classes;
import org.apache.openejb.testing.Module;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ApplicationComposer.class)
public class AggregateServiceIT {

    private static final UUID STREAM_ID = randomUUID();

    private static final String LIQUIBASE_SNAPSHOT_CHANGELOG_XML = "liquibase/snapshot-store-db-changelog.xml";

    private static final String LIQUIBASE_EVENT_STORE_CHANGELOG_XML = "liquibase/event-store-db-changelog.xml";

    @Resource(name = "openejb/Resource/eventStore")
    private DataSource dataSource;

    @Inject
    private SnapshotOpenEjbAwareJdbcRepository snapshotRepository;

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Module
    @Classes(cdi = true, value = {
            AggregateService.class,
            SnapshotOpenEjbAwareJdbcRepository.class,
            EventLogOpenEjbAwareJdbcRepository.class,
            EventSource.class,
            EnvelopeEventStream.class,
            EventStreamManager.class,
            LoggerProducer.class,
            EventRepository.class,
            EventLogConverter.class,
            StringToJsonObjectConverter.class,
            JsonObjectEnvelopeConverter.class,
            JsonObjectToObjectConverter.class,
            ObjectMapperProducer.class,
            JdbcEventRepository.class,
            JmsEventPublisher.class,
            DummyJmsEnvelopeSender.class,
            DefaultEventDestinationResolver.class,
            EnvelopeConverter.class,
            DefaultSnapshotService.class,
            DefaultSnapshotStrategy.class
    })

    public WebApp war() {
        return new WebApp()
                .contextRoot("snapshot-test")
                .addServlet("SnapShotApp", Application.class.getName());
    }

    @Before
    public void init() throws Exception {
        initDatabase();
    }

    private List<JsonEnvelope> createEnvelopes(int numberOfEnvelopes) {
        List<JsonEnvelope> envelopes = new LinkedList<>();
        for (int i = 1; i <= numberOfEnvelopes; i++) {
            envelopes.add(envelope().with(metadataWithRandomUUID("context.eventA").withStreamId(STREAM_ID)).withPayloadOf("value", "name").build());
        }
        return envelopes;
    }

    @Test
    public void shouldNotStoreABrandNewSnapshotWhenStrategyDoesNotMandateSavingSnapshot() throws DuplicateSnapshotException, InvalidSequenceIdException, EventStreamException {

        int snapshotCount =snapshotRepository.snapshotCount(STREAM_ID);
        assertThat(snapshotCount, is(0));

        aggregateService.register(new EventFoundEvent(EventA.class, "context.eventA"));

        EventStream stream = eventSource.getStreamById(STREAM_ID);
        stream.append(createEnvelopes(24).stream());

        aggregateService.get(stream, TestAggregate.class);

        Optional<AggregateSnapshot> snapshot = snapshotRepository.getLatestSnapshot(STREAM_ID);
        assertThat(snapshot, not(nullValue()));
        assertThat(snapshot.isPresent(), equalTo(false));

        snapshotCount =snapshotRepository.snapshotCount(STREAM_ID);
        assertThat(snapshotCount, is(0));
    }


    @Test
    public void shouldStoreABrandNewSnapshotWhenStrategyDoesMandateSavingSnapshot() throws DuplicateSnapshotException, InvalidSequenceIdException, EventStreamException {

        int snapshotCount =snapshotRepository.snapshotCount(STREAM_ID);
        assertThat(snapshotCount, is(0));

        aggregateService.register(new EventFoundEvent(EventA.class, "context.eventA"));

        EventStream stream = eventSource.getStreamById(STREAM_ID);
        stream.append(createEnvelopes(25).stream());

        aggregateService.get(stream, TestAggregate.class);

        Optional<AggregateSnapshot> snapshot = snapshotRepository.getLatestSnapshot(STREAM_ID);
        assertThat(snapshot, not(nullValue()));
        assertThat(snapshot.isPresent(), equalTo(true));
        assertThat(snapshot.get().getType(), equalTo(TestAggregate.class));
        assertThat(snapshot.get().getStreamId(), equalTo(STREAM_ID));
        assertThat(snapshot.get().getVersionId(), equalTo(25L));

        snapshotCount =snapshotRepository.snapshotCount(STREAM_ID);
        assertThat(snapshotCount, is(1));
    }


    @Test
    public void shouldNotStoreANewSnapshotOnTopOfExistingSnapshotsWhenStrategyDoesNotMandateSavingSnapshot() throws DuplicateSnapshotException, InvalidSequenceIdException, EventStreamException {

        int snapshotCount =snapshotRepository.snapshotCount(STREAM_ID);
        assertThat(snapshotCount, is(0));

        aggregateService.register(new EventFoundEvent(EventA.class, "context.eventA"));

        EventStream stream = eventSource.getStreamById(STREAM_ID);
        stream.append(createEnvelopes(25).stream());

        aggregateService.get(stream, TestAggregate.class);

        stream.append(createEnvelopes(23).stream());

        aggregateService.get(stream, TestAggregate.class);

        Optional<AggregateSnapshot> snapshot = snapshotRepository.getLatestSnapshot(STREAM_ID);
        assertThat(snapshot, not(nullValue()));
        assertThat(snapshot.isPresent(), equalTo(true));
        assertThat(snapshot.get().getType(), equalTo(TestAggregate.class));
        assertThat(snapshot.get().getStreamId(), equalTo(STREAM_ID));
        assertThat(snapshot.get().getVersionId(), equalTo(25l));


         snapshotCount =snapshotRepository.snapshotCount(STREAM_ID);
        assertThat(snapshotCount, is(1));
    }

    @Test
    public void shouldStoreANewSnapshotOnTopOfExistingSnapshotsWhenStrategyDoesMandateSavingSnapshot() throws DuplicateSnapshotException, InvalidSequenceIdException, EventStreamException {

        int snapshotCount =snapshotRepository.snapshotCount(STREAM_ID);
        assertThat(snapshotCount, is(0));

        aggregateService.register(new EventFoundEvent(EventA.class, "context.eventA"));

        EventStream stream = eventSource.getStreamById(STREAM_ID);
        stream.append(createEnvelopes(25).stream());

        aggregateService.get(stream, TestAggregate.class);

        stream.append(createEnvelopes(25).stream());

        aggregateService.get(stream, TestAggregate.class);

        Optional<AggregateSnapshot> snapshot = snapshotRepository.getLatestSnapshot(STREAM_ID);
        assertThat(snapshot, not(nullValue()));
        assertThat(snapshot.isPresent(), equalTo(true));
        assertThat(snapshot.get().getType(), equalTo(TestAggregate.class));
        assertThat(snapshot.get().getStreamId(), equalTo(STREAM_ID));
        assertThat(snapshot.get().getVersionId(), equalTo(50l));

        snapshotCount =snapshotRepository.snapshotCount(STREAM_ID);
        assertThat(snapshotCount, is(2));
    }

    @Event("eventA")
    public static class EventA {
        private String name;

        public EventA() {

        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private void initDatabase() throws Exception {

        Liquibase snapshotLiquidBase = new Liquibase(LIQUIBASE_SNAPSHOT_CHANGELOG_XML,
                new ClassLoaderResourceAccessor(), new JdbcConnection(dataSource.getConnection()));
        snapshotLiquidBase.dropAll();
        snapshotLiquidBase.update("");

        Liquibase eventStoreLiquidBase = new Liquibase(LIQUIBASE_EVENT_STORE_CHANGELOG_XML,
                new ClassLoaderResourceAccessor(), new JdbcConnection(dataSource.getConnection()));
        eventStoreLiquidBase.update("");

    }

    public static class TestAggregate implements Aggregate {
        private static final long serialVersionUID = 42L;

        public List<String> names = new ArrayList<>();

        @Override
        public Object apply(Object event) {
            names.add(((EventA) event).getName());
            return event;
        }
    }

    @ApplicationScoped
    public static class DummyJmsEnvelopeSender implements JmsEnvelopeSender {

        @Override
        public void send(JsonEnvelope envelope, Destination destination) {

        }

        @Override
        public void send(JsonEnvelope envelope, String destinationName) {

        }
    }
}
