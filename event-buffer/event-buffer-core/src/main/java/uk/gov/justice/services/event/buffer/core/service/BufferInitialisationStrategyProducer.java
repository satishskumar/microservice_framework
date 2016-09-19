package uk.gov.justice.services.event.buffer.core.service;


import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.event.buffer.core.repository.streamstatus.StreamStatusJdbcRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class BufferInitialisationStrategyProducer {

    private static final String TRUE = "true";
    @Inject
    @Value(key = "postgres.db", defaultValue = "true")
    String usePostgresStrategy;

    @Inject
    StreamStatusJdbcRepository streamStatusRepository;

    @Produces
    public BufferInitialisationStrategy bufferInitialisationStrategy() {
        return TRUE.equals(usePostgresStrategy)
                ? new PostgreSQLBasedBufferInitialisationStrategy(streamStatusRepository)
                : new AnsiSQLBasedBufferInitialisationStrategy(streamStatusRepository);
    }

}
