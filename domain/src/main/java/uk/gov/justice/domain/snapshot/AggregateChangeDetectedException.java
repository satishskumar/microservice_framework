package uk.gov.justice.domain.snapshot;

public class AggregateChangeDetectedException extends Exception {
    private static final long serialVersionUID = 5934757852541650746L;

    public AggregateChangeDetectedException(String message) {
        super(message);
    }
}
