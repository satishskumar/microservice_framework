package uk.gov.justice.services.eventsourcing.source.core.exception;

public class AggregateCreationException extends RuntimeException {

    public static final String ACCESS_ERROR = "Unable to create aggregate due to access error";

    public static final String CONSTRUCTION_ERROR = "Unable to create aggregate due to non instantiable class";

    public AggregateCreationException(final IllegalAccessException e) {
        super(ACCESS_ERROR, e);
    }

    public AggregateCreationException(final InstantiationException e) {
        super(CONSTRUCTION_ERROR, e);
    }
}
