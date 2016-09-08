package uk.gov.justice.services.eventsourcing.source.core.snapshot;

public interface SnapshotStrategy {
     boolean shouldCreateSnapshot(final long aggregateVersionId,
                                  final long snapshotVersionId);


}
