package uk.gov.justice.services.event.buffer.it.util.repository;

import uk.gov.justice.services.event.buffer.core.repository.streambuffer.StreamBufferJdbcRepository;

public class StreamBufferOpenEjbAwareJdbcRepository extends StreamBufferJdbcRepository {
    @Override
    protected String jndiName() {
        return "java:openejb/Resource/viewStore";
    }
}
