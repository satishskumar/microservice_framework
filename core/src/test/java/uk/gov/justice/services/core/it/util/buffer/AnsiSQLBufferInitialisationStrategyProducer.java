package uk.gov.justice.services.core.it.util.buffer;


import uk.gov.justice.services.event.buffer.core.repository.streamstatus.StreamStatusJdbcRepository;
import uk.gov.justice.services.event.buffer.core.service.BufferInitialisationStrategy;
import uk.gov.justice.services.event.buffer.core.service.AnsiSQLBasedBufferInitialisationStrategy;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class AnsiSQLBufferInitialisationStrategyProducer {

    @Inject
    StreamStatusJdbcRepository streamStatusRepository;

    @Produces
    public BufferInitialisationStrategy bufferInitialisationStrategy() {
        return new AnsiSQLBasedBufferInitialisationStrategy(streamStatusRepository);
    }
}
