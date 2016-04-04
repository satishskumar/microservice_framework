package uk.gov.justice.services.event.enveloper.exception;

import uk.gov.justice.services.core.annotation.Event;
import uk.gov.justice.services.event.enveloper.Enveloper;

/**
 * Exception thrown when {@link Enveloper} receives an invalid {@link Event} object.
 */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(final String message) {
        super(message);
    }

}
