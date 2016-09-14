package uk.gov.justice.services.eventsourcing.source.core.snapshot;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class DefaultSnapshotStrategyTest {

    @Mock
    Logger logger;

    @InjectMocks
    private DefaultSnapshotStrategy snapshotStrategy;

    @Before
    public void setUp() {
        snapshotStrategy.logger = logger;
    }

    @Test
    public void shouldCreateAggregateSnapshotWhenDifferenceGreaterThanThreshold() {
        long streamVersionId = 26l;
        long snapshotVersionId = 0l;
        boolean canBeCreated = snapshotStrategy.shouldCreateSnapshot(streamVersionId, snapshotVersionId);

        assertEquals(true, canBeCreated);
    }

    @Test
    public void shouldNotCreateAggregateSnapshotWhenDifferenceIsEqualToThreshold() {

        long streamVersionId = 25l;
        long snapshotVersionId = 0l;
        boolean canBeCreated = snapshotStrategy.shouldCreateSnapshot(streamVersionId, snapshotVersionId);

        assertEquals(true, canBeCreated);
    }

    @Test
    public void shouldNotCreateAggregageSnapshotWhenDifferenceIsLessThanThreshold() {

        long streamVersionId = 24l;
        long snapshotVersionId = 0l;
        boolean canBeCreated = snapshotStrategy.shouldCreateSnapshot(streamVersionId, snapshotVersionId);

        assertEquals(false, canBeCreated);
    }

    @Test
    public void shouldNotCreateAggregageSnapshotWhenDifferenceIsLessThanThresholdWhenSnapshotIsAlreadyAvailable(){

        long streamVersionId = 51l;
        long snapshotVersionId = 25l;
        boolean canBeCreated = snapshotStrategy.shouldCreateSnapshot(streamVersionId, snapshotVersionId);

        assertEquals(true, canBeCreated);
    }

    @Test
    public void shouldCreateAggregageSnapshotWhenDifferenceIsEqualToThresholdWhenSnapshotIsAlreadyAvailable(){

        long streamVersionId = 50l;
        long snapshotVersionId = 25l;
        boolean canBeCreated = snapshotStrategy.shouldCreateSnapshot(streamVersionId, snapshotVersionId);

        assertEquals(true, canBeCreated);
    }

    @Test
    public void shouldCreateAggregageSnapshotWhenDifferenceIsLessThanThresholdWhenSnapshotIsAlreadyAvailable() {

        long streamVersionId = 49l;
        long snapshotVersionId = 25l;
        boolean canBeCreated = snapshotStrategy.shouldCreateSnapshot(streamVersionId, snapshotVersionId);

        assertEquals(false, canBeCreated);
    }
}
