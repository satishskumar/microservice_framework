package uk.gov.justice.services.eventsourcing.repository.core.exception;

public class DuplicateSnapshotException extends Exception {
    private static final long serialVersionUID = 5934757852541630746L;

    public DuplicateSnapshotException(String message) {
        super(message);
    }

}

