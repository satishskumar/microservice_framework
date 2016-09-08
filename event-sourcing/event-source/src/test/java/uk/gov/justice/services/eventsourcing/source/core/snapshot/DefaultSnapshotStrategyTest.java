package uk.gov.justice.services.eventsourcing.source.core.snapshot;

import static org.junit.Assert.assertEquals;

import uk.gov.justice.services.eventsourcing.repository.core.exception.DuplicateSnapshotException;
import uk.gov.justice.services.eventsourcing.repository.core.exception.InvalidSequenceIdException;

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
    public void shouldCreateAggregateSnapshotWhenDifferenceGreaterThanThreshold() throws
            DuplicateSnapshotException, InvalidSequenceIdException, IllegalAccessException, InstantiationException {

        long streamVersionId = 26l;
        long snapshotVersionId = 0l;
        boolean canBeCreated = snapshotStrategy.shouldCreateSnapshot(streamVersionId, snapshotVersionId);

        assertEquals(true, canBeCreated);
    }

    @Test
    public void shouldNotCreateAggregateSnapshotWhenDifferenceIsEqualToThreshold()
            throws
            DuplicateSnapshotException, InvalidSequenceIdException, IllegalAccessException, InstantiationException {

        long streamVersionId = 25l;
        long snapshotVersionId = 0l;
        boolean canBeCreated = snapshotStrategy.shouldCreateSnapshot(streamVersionId, snapshotVersionId);

        assertEquals(true, canBeCreated);
    }

    @Test
    public void shouldNotCreateAggregageSnapshotWhenDifferenceIsLessThanThreshold()
            throws
            DuplicateSnapshotException, InvalidSequenceIdException, IllegalAccessException, InstantiationException {

        long streamVersionId = 24l;
        long snapshotVersionId = 0l;
        boolean canBeCreated = snapshotStrategy.shouldCreateSnapshot(streamVersionId, snapshotVersionId);

        assertEquals(false, canBeCreated);
    }

    @Test
    public void shouldNotCreateAggregageSnapshotWhenDifferenceIsLessThanThresholdWhenSnapshotIsAlreadyAvailable()
            throws
            DuplicateSnapshotException, InvalidSequenceIdException, IllegalAccessException, InstantiationException {

        long streamVersionId = 51l;
        long snapshotVersionId = 25l;
        boolean canBeCreated = snapshotStrategy.shouldCreateSnapshot(streamVersionId, snapshotVersionId);

        assertEquals(true, canBeCreated);
    }

    @Test
    public void shouldCreateAggregageSnapshotWhenDifferenceIsEqualToThresholdWhenSnapshotIsAlreadyAvailable()
            throws
            DuplicateSnapshotException, InvalidSequenceIdException, IllegalAccessException, InstantiationException {

        long streamVersionId = 50l;
        long snapshotVersionId = 25l;
        boolean canBeCreated = snapshotStrategy.shouldCreateSnapshot(streamVersionId, snapshotVersionId);

        assertEquals(true, canBeCreated);
    }

    @Test
    public void shouldCreateAggregageSnapshotWhenDifferenceIsLessThanThresholdWhenSnapshotIsAlreadyAvailable()
            throws
            DuplicateSnapshotException, InvalidSequenceIdException, IllegalAccessException, InstantiationException {

        long streamVersionId = 49l;
        long snapshotVersionId = 25l;
        boolean canBeCreated = snapshotStrategy.shouldCreateSnapshot(streamVersionId, snapshotVersionId);

        assertEquals(false, canBeCreated);
    }
}
