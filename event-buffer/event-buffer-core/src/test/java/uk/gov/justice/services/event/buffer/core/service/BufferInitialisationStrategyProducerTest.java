package uk.gov.justice.services.event.buffer.core.service;


import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class BufferInitialisationStrategyProducerTest {

    private BufferInitialisationStrategyProducer strategyProducer = new BufferInitialisationStrategyProducer();

    @Test
    public void shouldProducePostgresStrategy() throws Exception {
        strategyProducer.usePostgresStrategy = "true";
        assertThat(strategyProducer.bufferInitialisationStrategy(), instanceOf(PostgreSQLBasedBufferInitialisationStrategy.class));
    }

    @Test
    public void shouldProduceAnsiSQLStrategy() throws Exception {
        strategyProducer.usePostgresStrategy = "false";
        assertThat(strategyProducer.bufferInitialisationStrategy(), instanceOf(AnsiSQLBasedBufferInitialisationStrategy.class));
    }

}