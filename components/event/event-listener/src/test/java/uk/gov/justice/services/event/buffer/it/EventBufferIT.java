package uk.gov.justice.services.event.buffer.it;

import static co.unruly.matchers.OptionalMatchers.contains;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelope;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.accesscontrol.AccessControlFailureMessageGenerator;
import uk.gov.justice.services.core.accesscontrol.AccessControlService;
import uk.gov.justice.services.core.accesscontrol.AllowAllPolicyEvaluator;
import uk.gov.justice.services.core.accesscontrol.PolicyEvaluator;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.cdi.LoggerProducer;
import uk.gov.justice.services.core.dispatcher.DispatcherCache;
import uk.gov.justice.services.core.dispatcher.DispatcherFactory;
import uk.gov.justice.services.core.dispatcher.EmptySystemUserProvider;
import uk.gov.justice.services.core.dispatcher.RequesterProducer;
import uk.gov.justice.services.core.dispatcher.ServiceComponentObserver;
import uk.gov.justice.services.core.dispatcher.SystemUserUtil;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.extension.AnnotationScanner;
import uk.gov.justice.services.core.extension.BeanInstantiater;
import uk.gov.justice.services.core.interceptor.InterceptorCache;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessorProducer;
import uk.gov.justice.services.core.interceptor.InterceptorObserver;
import uk.gov.justice.services.core.jms.DefaultJmsDestinations;
import uk.gov.justice.services.core.jms.JmsSenderFactory;
import uk.gov.justice.services.core.sender.ComponentDestination;
import uk.gov.justice.services.core.sender.SenderProducer;
import uk.gov.justice.services.event.buffer.EventBufferInterceptor;
import uk.gov.justice.services.event.buffer.core.repository.streambuffer.StreamBufferEvent;
import uk.gov.justice.services.event.buffer.core.repository.streamstatus.StreamStatus;
import uk.gov.justice.services.event.buffer.core.service.ConsecutiveEventBufferService;
import uk.gov.justice.services.event.buffer.it.util.repository.StreamBufferOpenEjbAwareJdbcRepository;
import uk.gov.justice.services.event.buffer.it.util.repository.StreamStatusOpenEjbAwareJdbcRepository;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.jms.DefaultJmsEnvelopeSender;
import uk.gov.justice.services.messaging.jms.EnvelopeConverter;
import uk.gov.justice.services.test.utils.common.envelope.TestEnvelopeRecorder;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.openejb.jee.Application;
import org.apache.openejb.jee.WebApp;
import org.apache.openejb.junit.ApplicationComposer;
import org.apache.openejb.testing.Classes;
import org.apache.openejb.testing.Module;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ApplicationComposer.class)
@Adapter(EVENT_LISTENER)
public class EventBufferIT {

    private static final String EVENT_ABC = "test.event-abc";
    private static final String LIQUIBASE_STREAM_STATUS_CHANGELOG_XML = "liquibase/event-buffer-changelog.xml";

    @Resource(name = "openejb/Resource/viewStore")
    private DataSource dataSource;

    @Inject
    private InterceptorChainProcessor interceptorChainProcessor;

    @Inject
    private AbcEventHandler abcEventHandler;

    @Inject
    private StreamBufferOpenEjbAwareJdbcRepository jdbcStreamBufferRepository;

    @Inject
    private StreamStatusOpenEjbAwareJdbcRepository statusRepository;

    @Module
    @Classes(cdi = true, value = {
            AbcEventHandler.class,
            AnnotationScanner.class,
            RequesterProducer.class,
            ServiceComponentObserver.class,

            InterceptorChainProcessorProducer.class,
            InterceptorChainProcessor.class,
            InterceptorCache.class,
            InterceptorObserver.class,
            EventBufferInterceptor.class,

            SenderProducer.class,
            JmsSenderFactory.class,
            ComponentDestination.class,
            DefaultJmsEnvelopeSender.class,
            DefaultJmsDestinations.class,
            EnvelopeConverter.class,

            StringToJsonObjectConverter.class,
            JsonObjectEnvelopeConverter.class,
            ObjectToJsonValueConverter.class,
            ObjectMapperProducer.class,
            Enveloper.class,

            AccessControlFailureMessageGenerator.class,
            AllowAllPolicyEvaluator.class,
            AccessControlService.class,
            DispatcherCache.class,
            DispatcherFactory.class,
            PolicyEvaluator.class,

            StreamBufferOpenEjbAwareJdbcRepository.class,
            StreamStatusOpenEjbAwareJdbcRepository.class,
            ConsecutiveEventBufferService.class,
            LoggerProducer.class,
            EmptySystemUserProvider.class,
            SystemUserUtil.class,
            BeanInstantiater.class

    })
    public WebApp war() {
        return new WebApp()
                .contextRoot("core-test")
                .addServlet("TestApp", Application.class.getName());
    }

    @Before
    public void init() throws Exception {
        initDatabase();
    }

    @Test
    public void shouldAddEventToBufferIfVersionNotOne() {

        UUID metadataId = UUID.randomUUID();
        UUID streamId = UUID.randomUUID();
        final JsonEnvelope envelope = envelope()
                .with(metadataOf(metadataId, EVENT_ABC)
                        .withStreamId(streamId).withVersion(2L))
                .build();
        interceptorChainProcessor.process(envelope);

        List<StreamBufferEvent> streamBufferEvents = jdbcStreamBufferRepository.streamById(streamId).collect(toList());

        assertThat(streamBufferEvents, hasSize(1));
        assertThat(streamBufferEvents.get(0).getStreamId(), is(streamId));
        assertThat(streamBufferEvents.get(0).getVersion(), is(2L));

        final List<JsonEnvelope> handledEnvelopes = abcEventHandler.recordedEnvelopes();
        assertThat(handledEnvelopes, empty());
    }

    @Test
    public void shouldAddStatusVersionForNewStreamIdAndProcessIncomingEvent() {

        UUID metadataId = UUID.randomUUID();
        UUID streamId = UUID.randomUUID();
        final JsonEnvelope envelope = envelope()
                .with(metadataOf(metadataId, EVENT_ABC)
                        .withStreamId(streamId).withVersion(1L))
                .build();
        interceptorChainProcessor.process(envelope);

        List<StreamBufferEvent> streamBufferEvents = jdbcStreamBufferRepository.streamById(streamId).collect(toList());
        Optional<StreamStatus> streamStatus = statusRepository.findByStreamId(streamId);

        assertThat(streamBufferEvents, empty());
        assertThat(streamStatus.isPresent(), is(true));
        assertThat(streamStatus.get().getVersion(), is(1L));

        final List<JsonEnvelope> handledEnvelopes = abcEventHandler.recordedEnvelopes();
        assertThat(handledEnvelopes, hasSize(1));

        assertThat(handledEnvelopes.get(0).metadata().id(), is(metadataId));
        assertThat(handledEnvelopes.get(0).metadata().version(), contains(1L));
    }

    @Test
    public void shouldIncrementVersionWhenEventInOrder() throws SQLException, NamingException {

        UUID metadataId = UUID.randomUUID();
        UUID streamId = UUID.randomUUID();

        statusRepository.insert(new StreamStatus(streamId, 1L));


        final JsonEnvelope envelope = envelope()
                .with(metadataOf(metadataId, EVENT_ABC)
                        .withStreamId(streamId).withVersion(2L))
                .build();


        interceptorChainProcessor.process(envelope);

        List<StreamBufferEvent> streamBufferEvents = jdbcStreamBufferRepository.streamById(streamId).collect(toList());
        Optional<StreamStatus> streamStatus = statusRepository.findByStreamId(streamId);

        assertThat(streamBufferEvents, empty());
        assertThat(streamStatus.isPresent(), is(true));
        assertThat(streamStatus.get().getVersion(), is(2L));

    }

    @Test
    public void shouldNotIncrementVersionWhenEventNotInOrder() throws SQLException, NamingException {

        UUID metadataId = UUID.randomUUID();
        UUID streamId = UUID.randomUUID();

        statusRepository.insert(new StreamStatus(streamId, 2L));

        interceptorChainProcessor.process(envelope()
                .with(metadataOf(metadataId, EVENT_ABC)
                        .withStreamId(streamId).withVersion(4L))
                .build());

        List<StreamBufferEvent> streamBufferEvents = jdbcStreamBufferRepository.streamById(streamId).collect(toList());
        Optional<StreamStatus> streamStatus = statusRepository.findByStreamId(streamId);

        assertThat(streamBufferEvents, hasSize(1));
        assertThat(streamBufferEvents.get(0).getStreamId(), is(streamId));
        assertThat(streamBufferEvents.get(0).getVersion(), is(4L));
        assertThat(streamStatus.isPresent(), is(true));
        assertThat(streamStatus.get().getVersion(), is(2L));

    }

    @Test
    public void shouldReleaseBufferWhenMissingEventArrives() throws SQLException, NamingException {

        UUID metadataId2 = UUID.randomUUID();
        UUID streamId = UUID.randomUUID();
        final JsonEnvelope envelope = envelope()
                .with(metadataOf(metadataId2, EVENT_ABC)
                        .withStreamId(streamId).withVersion(2L))
                .build();


        statusRepository.insert(new StreamStatus(streamId, 1L));

        UUID metadataId3 = UUID.randomUUID();
        UUID metadataId4 = UUID.randomUUID();
        UUID metadataId5 = UUID.randomUUID();

        jdbcStreamBufferRepository.insert(
                new StreamBufferEvent(streamId, 3L, envelope()
                        .with(metadataOf(metadataId3, EVENT_ABC)
                                .withStreamId(streamId).withVersion(3L)).toJsonString()
                )
        );

        jdbcStreamBufferRepository.insert(
                new StreamBufferEvent(streamId, 4L, envelope()
                        .with(metadataOf(metadataId4, EVENT_ABC)
                                .withStreamId(streamId).withVersion(4L)).toJsonString()
                )
        );

        jdbcStreamBufferRepository.insert(
                new StreamBufferEvent(streamId, 5L, envelope()
                        .with(metadataOf(metadataId5, EVENT_ABC)
                                .withStreamId(streamId).withVersion(5L)).toJsonString()
                )
        );

        interceptorChainProcessor.process(envelope);

        List<StreamBufferEvent> streamBufferEvents = jdbcStreamBufferRepository.streamById(streamId).collect(toList());
        Optional<StreamStatus> streamStatus = statusRepository.findByStreamId(streamId);

        assertThat(streamStatus.isPresent(), is(true));
        assertThat(streamStatus.get().getVersion(), is(5L));

        final List<JsonEnvelope> handledEnvelopes = abcEventHandler.recordedEnvelopes();
        assertThat(handledEnvelopes, hasSize(4));

        assertThat(handledEnvelopes.get(0).metadata().id(), is(metadataId2));
        assertThat(handledEnvelopes.get(0).metadata().version(), contains(2L));

        assertThat(handledEnvelopes.get(1).metadata().id(), is(metadataId3));
        assertThat(handledEnvelopes.get(1).metadata().version(), contains(3L));

        assertThat(handledEnvelopes.get(2).metadata().id(), is(metadataId4));
        assertThat(handledEnvelopes.get(2).metadata().version(), contains(4L));

        assertThat(handledEnvelopes.get(3).metadata().id(), is(metadataId5));
        assertThat(handledEnvelopes.get(3).metadata().version(), contains(5L));


        assertThat(streamBufferEvents, hasSize(0));
    }

    @Test
    public void shouldIgnoreEventWithSupersededVersion() throws SQLException, NamingException {

        UUID metadataId = UUID.randomUUID();
        UUID streamId = UUID.randomUUID();
        final JsonEnvelope envelope = envelope()
                .with(metadataOf(metadataId, EVENT_ABC)
                        .withStreamId(streamId).withVersion(1L))
                .build();

        final StreamBufferEvent streamBufferEvent2 = new StreamBufferEvent(streamId, 4L, "payload");
        final StreamBufferEvent streamBufferEvent3 = new StreamBufferEvent(streamId, 5L, "payload");


        statusRepository.insert(new StreamStatus(streamId, 2L));
        jdbcStreamBufferRepository.insert(streamBufferEvent2);
        jdbcStreamBufferRepository.insert(streamBufferEvent3);

        interceptorChainProcessor.process(envelope);

        List<StreamBufferEvent> streamBufferEvents = jdbcStreamBufferRepository.streamById(streamId).collect(toList());
        Optional<StreamStatus> streamStatus = statusRepository.findByStreamId(streamId);

        assertThat(streamBufferEvents, hasSize(2));
        assertThat(streamStatus.isPresent(), is(true));
        assertThat(streamStatus.get().getVersion(), is(2L));
        assertThat(abcEventHandler.recordedEnvelopes(), empty());
    }

    private void initDatabase() throws Exception {
        Liquibase liquibase = new Liquibase(LIQUIBASE_STREAM_STATUS_CHANGELOG_XML,
                new ClassLoaderResourceAccessor(), new JdbcConnection(dataSource.getConnection()));
        liquibase.dropAll();
        liquibase.update("");
    }

    @ServiceComponent(EVENT_LISTENER)
    @ApplicationScoped
    public static class AbcEventHandler extends TestEnvelopeRecorder {

        @Handles(EVENT_ABC)
        public void handle(JsonEnvelope envelope) {
            record(envelope);
        }

    }

}
