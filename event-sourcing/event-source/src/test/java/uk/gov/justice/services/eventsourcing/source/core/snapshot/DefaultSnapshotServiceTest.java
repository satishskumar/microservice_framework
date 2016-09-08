package uk.gov.justice.services.eventsourcing.source.core.snapshot;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import uk.gov.justice.domain.snapshot.AggregateSnapshot;
import uk.gov.justice.services.eventsourcing.repository.core.SnapshotRepository;
import uk.gov.justice.services.eventsourcing.repository.core.exception.DuplicateSnapshotException;
import uk.gov.justice.services.eventsourcing.repository.core.exception.InvalidSequenceIdException;
import uk.gov.justice.services.eventsourcing.source.core.exception.AggregateCreationException;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang.SerializationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class DefaultSnapshotServiceTest {

    private static final UUID STREAM_ID = randomUUID();
    private static final long INITIAL_AGGREGATE_VERSION = 0;

    @Mock
    SnapshotRepository snapshotRepository;

    @Mock
    SnapshotStrategy snapshotStrategy;

    @Mock
    Logger logger;

    @Captor
    ArgumentCaptor<AggregateSnapshot<TestAggregate>> snapshotArgumentCaptor;

    @InjectMocks
    private DefaultSnapshotService snapshotService;


    @Test
    public void shouldCreateAggregate() throws DuplicateSnapshotException, InvalidSequenceIdException {
        final Optional<AggregateSnapshot> aggregateSnapshot = Optional.empty();
        when(snapshotRepository.getLatestSnapshot(STREAM_ID)).thenReturn(aggregateSnapshot);

        VersionedAggregate<TestAggregate> versionedAggregate1 = snapshotService.getLatestVersionedAggregate(STREAM_ID, TestAggregate.class);

        assertThat(versionedAggregate1, notNullValue());
        assertThat(versionedAggregate1.getVersionId(), is(INITIAL_AGGREGATE_VERSION));
        assertThat(versionedAggregate1.getAggregate().state, nullValue());
    }


    @Test
    public void shouldCreateSnapshotIfStrategyMandatesCreation() throws DuplicateSnapshotException, InvalidSequenceIdException {
        TestAggregate aggregate = new TestAggregate("someState");
        final Optional<AggregateSnapshot> aggregateSnapshot = Optional.empty();
        final Long initialAggregateVersionId = 0l;
        final Long currentAggregateVersionId = 26l;
        when(snapshotRepository.getLatestSnapshot(STREAM_ID)).thenReturn(aggregateSnapshot);
        when(snapshotStrategy.shouldCreateSnapshot(currentAggregateVersionId, initialAggregateVersionId)).thenReturn(true);

        snapshotService.attemptAggregateStore(STREAM_ID, currentAggregateVersionId, TestAggregate.class, aggregate, initialAggregateVersionId);

        verify(snapshotRepository, times(1)).storeSnapshot(snapshotArgumentCaptor.capture());

        assertThat(snapshotArgumentCaptor.getValue(), notNullValue());
        assertThat(snapshotArgumentCaptor.getValue().getVersionId(), is(currentAggregateVersionId));
        assertThat(snapshotArgumentCaptor.getValue().getAggregate().state, is("someState"));
    }

    @Test
    public void shouldNotCreateAggregateIfStrategyDoesNotMandatesCreation() throws DuplicateSnapshotException, InvalidSequenceIdException {
        final Long initialAggregateVersionId = 0l;
        final Long currentAggregateVersionId = 26l;
        final Optional<AggregateSnapshot> aggregateSnapshot = Optional.empty();

        TestAggregate aggregate = new TestAggregate(null);
        when(snapshotStrategy.shouldCreateSnapshot(currentAggregateVersionId, initialAggregateVersionId)).thenReturn(false);

        snapshotService.attemptAggregateStore(STREAM_ID, currentAggregateVersionId, TestAggregate.class, aggregate, initialAggregateVersionId);

        verifyZeroInteractions(snapshotRepository);
    }

    @Test
    public void shouldNotCreateSnapshotWhenStrategyMandatesCreationButFailsSerialization() throws DuplicateSnapshotException, InvalidSequenceIdException {
        NoSerializableTestAggregate aggregate = new NoSerializableTestAggregate();
        final Long initialAggregateVersionId = 0l;
        final Long currentAggregateVersionId = 26l;

        when(snapshotStrategy.shouldCreateSnapshot(currentAggregateVersionId, initialAggregateVersionId)).thenReturn(true);

        snapshotService.attemptAggregateStore(STREAM_ID, currentAggregateVersionId, NoSerializableTestAggregate.class, aggregate, initialAggregateVersionId);

        verifyZeroInteractions(snapshotRepository);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldThrowExceptionWhenInstantiationOfAggregateFailsDuetToAccess() {
        expectedException.expect(AggregateCreationException.class);
        expectedException.expectMessage(AggregateCreationException.ACCESS_ERROR);

        final Optional<AggregateSnapshot> aggregateSnapshot = Optional.empty();
        when(snapshotRepository.getLatestSnapshot(STREAM_ID)).thenReturn(aggregateSnapshot);

        snapshotService.getLatestVersionedAggregate(STREAM_ID, IllegalAccessTestAggregate.class);
    }

    @Test
    public void shouldThrowWhenInstantiationOfAggregateFails() {
        expectedException.expect(AggregateCreationException.class);
        expectedException.expectMessage(AggregateCreationException.CONSTRUCTION_ERROR);

        final Optional<AggregateSnapshot> aggregateSnapshot = Optional.empty();
        when(snapshotRepository.getLatestSnapshot(STREAM_ID)).thenReturn(aggregateSnapshot);

        snapshotService.getLatestVersionedAggregate(STREAM_ID, NonInstantiatableTestAggregate.class);
    }


    @Test
    public void shouldCreateNewAggregateWhenNoEarlierSnapshotsWhenSerializationFailsWithLatestSnapshot() throws InstantiationException, IllegalAccessException {
        final AggregateSnapshot mockAggregateSnapshot = mock(AggregateSnapshot.class);
        TestAggregate aggregate = new TestAggregate("someState");
        final Optional<AggregateSnapshot> aggregateSnapshot = Optional.of(mockAggregateSnapshot);
        when(mockAggregateSnapshot.getStreamId()).thenReturn(STREAM_ID);
        when(mockAggregateSnapshot.getVersionId()).thenReturn(1l);
        when(mockAggregateSnapshot.getAggregate()).thenThrow(SerializationException.class);
        when(snapshotRepository.getLatestSnapshot(STREAM_ID)).thenReturn(aggregateSnapshot);
        when(snapshotRepository.getEarlierSnapshot(STREAM_ID, 1l)).thenReturn(Optional.empty());

        VersionedAggregate<TestAggregate> versionedAggregate = snapshotService.getLatestVersionedAggregate(STREAM_ID, TestAggregate.class);

        assertThat(versionedAggregate, notNullValue());
        assertThat(versionedAggregate.getVersionId(), is(INITIAL_AGGREGATE_VERSION));
        verify(snapshotRepository, times(1)).getEarlierSnapshot(STREAM_ID, 1l);
    }

    @Test
    public void shouldCreateAnAggregateFromPreviousSnapshotWhenSerializationFailsWithLatestSnapshot() throws InstantiationException, IllegalAccessException {

        TestAggregate aggregate = new TestAggregate("someState");


        final AggregateSnapshot latestSnapshot = mock(AggregateSnapshot.class);
        when(latestSnapshot.getStreamId()).thenReturn(STREAM_ID);
        when(latestSnapshot.getVersionId()).thenReturn(6l);
        when(latestSnapshot.getAggregate()).thenThrow(SerializationException.class);

        when(snapshotRepository.getLatestSnapshot(STREAM_ID)).thenReturn(Optional.of(latestSnapshot));

        final AggregateSnapshot<TestAggregate> earlierSnapshot = new AggregateSnapshot<>(STREAM_ID, 5l, TestAggregate.class, aggregate);
        when(snapshotRepository.getEarlierSnapshot(STREAM_ID, 6l)).thenReturn(Optional.of(earlierSnapshot));

        VersionedAggregate<TestAggregate> versionedAggregate = snapshotService.getLatestVersionedAggregate(STREAM_ID, TestAggregate.class);

        assertThat(versionedAggregate, notNullValue());
        assertThat(versionedAggregate.getVersionId(), is(5l));

    }
}
