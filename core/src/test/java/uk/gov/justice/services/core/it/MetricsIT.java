package uk.gov.justice.services.core.it;


import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelope;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.accesscontrol.AccessControlFailureMessageGenerator;
import uk.gov.justice.services.core.accesscontrol.AccessControlService;
import uk.gov.justice.services.core.accesscontrol.AllowAllPolicyEvaluator;
import uk.gov.justice.services.core.accesscontrol.PolicyEvaluator;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.cdi.LoggerProducer;
import uk.gov.justice.services.core.configuration.ServiceContextNameProvider;
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
import uk.gov.justice.services.core.metrics.MetricRegistryProducer;
import uk.gov.justice.services.core.metrics.MetricsInterceptor;
import uk.gov.justice.services.core.sender.ComponentDestination;
import uk.gov.justice.services.core.sender.SenderProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.jms.DefaultJmsEnvelopeSender;
import uk.gov.justice.services.messaging.jms.EnvelopeConverter;

import java.lang.management.ManagementFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.openejb.jee.Application;
import org.apache.openejb.jee.WebApp;
import org.apache.openejb.junit.ApplicationComposer;
import org.apache.openejb.testing.Classes;
import org.apache.openejb.testing.Module;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(ApplicationComposer.class)
@FrameworkComponent("CORE_TEST")
@Adapter(EVENT_LISTENER)
public class MetricsIT {

    private static final String EVENT_ABC = "test.event-abc";

    @Inject
    private InterceptorChainProcessor interceptorChainProcessor;

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

            SenderProducer.class,
            JmsSenderFactory.class,
            ComponentDestination.class,
            DefaultJmsEnvelopeSender.class,
            DefaultJmsDestinations.class,
            EnvelopeConverter.class,

            StringToJsonObjectConverter.class,
            JsonObjectEnvelopeConverter.class,
            ObjectToJsonValueConverter.class,
            ObjectMapper.class,
            Enveloper.class,

            AccessControlFailureMessageGenerator.class,
            AllowAllPolicyEvaluator.class,
            AccessControlService.class,
            DispatcherCache.class,
            DispatcherFactory.class,
            PolicyEvaluator.class,

            LoggerProducer.class,
            EmptySystemUserProvider.class,
            BeanInstantiater.class,
            MetricRegistryProducer.class,
            MetricsInterceptor.class,
            SystemUserUtil.class,
            TestServiceContextNameProvider.class
    })
    public WebApp war() {
        return new WebApp()
                .contextRoot("core-test")
                .addServlet("TestApp", Application.class.getName());
    }

    @Test
    public void shouldExposeComponentMetrics() throws Exception {
        interceptorChainProcessor.process(envelope()
                .with(metadataWithRandomUUID(EVENT_ABC)).build());

        interceptorChainProcessor.process(envelope()
                .with(metadataWithRandomUUID(EVENT_ABC)).build());

        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName metricsObjectName = new ObjectName("metrics:name=test-component");

        final Object count = server.getAttribute(metricsObjectName, "Count");
        final Object mean = server.getAttribute(metricsObjectName, "Mean");
        final Object max = server.getAttribute(metricsObjectName, "Max");
        final Object min = server.getAttribute(metricsObjectName, "Min");

        assertThat(count, is(2L));
        assertThat(mean, not(nullValue()));
        assertThat(max, not(nullValue()));
        assertThat(min, not(nullValue()));

    }

    @ApplicationScoped
    public static class TestServiceContextNameProvider implements ServiceContextNameProvider {

        @Override
        public String getServiceContextName() {
            return "test-component";
        }
    }

    @ServiceComponent(EVENT_LISTENER)
    @ApplicationScoped
    public static class AbcEventHandler {

        @Handles(EVENT_ABC)
        public void handle(JsonEnvelope envelope) {

        }
    }
}
