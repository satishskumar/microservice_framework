package uk.gov.justice.services.eventsourcing.repository.jdbc.snapshot;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.domain.snapshot.AggregateSnapshot;
import uk.gov.justice.services.eventsourcing.repository.core.exception.DuplicateSnapshotException;
import uk.gov.justice.services.eventsourcing.repository.core.exception.InvalidSequenceIdException;
import uk.gov.justice.services.jdbc.persistence.JdbcRepositoryException;
import uk.gov.justice.services.test.utils.persistence.AbstractJdbcRepositoryIT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SnapshotRepositoryJdbcIT extends AbstractJdbcRepositoryIT<SnapshotJdbcRepository> {

    public class RecordingAggregate implements Aggregate {

        List<Object> recordedEvents = new ArrayList<>();

        @Override
        public Object apply(Object event) {
            recordedEvents.add(event);
            return event;
        }
    }
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final UUID STREAM_ID = randomUUID();
    private static final Long VERSION_ID = 5L;
    private static final Class<? extends Aggregate> TYPE = RecordingAggregate.class;
    private static final byte[] AGGREGATE = "Any String you want".getBytes();

    private static final String LIQUIBASE_SNAPSHOT_STORE_DB_CHANGELOG_XML = "liquibase/snapshot-store-db-changelog.xml";

    public SnapshotRepositoryJdbcIT() {
        super(LIQUIBASE_SNAPSHOT_STORE_DB_CHANGELOG_XML);
    }

    @Before
    public void initializeDependencies() throws Exception {
        jdbcRepository = new SnapshotJdbcRepository();
        registerDataSource();
    }

    @Test
    public void shouldStoreAndRetrieveSnapshot() throws DuplicateSnapshotException, InvalidSequenceIdException {

        AggregateSnapshot aggregateSnapshot = createSnapshot(STREAM_ID, VERSION_ID, TYPE, AGGREGATE);

        jdbcRepository.storeSnapshot(aggregateSnapshot);

        Optional<AggregateSnapshot> snapshot = jdbcRepository.getLatestSnapshot(STREAM_ID);

        assertThat(snapshot, notNullValue());
        assertThat(snapshot, is(Optional.of(aggregateSnapshot)));
    }

    @Test
    public void shouldRetrieveLatestSnapshot() throws DuplicateSnapshotException, InvalidSequenceIdException {

        AggregateSnapshot aggregateSnapshot1 = createSnapshot(STREAM_ID, VERSION_ID + 1, TYPE, AGGREGATE);
        AggregateSnapshot aggregateSnapshot2 = createSnapshot(STREAM_ID, VERSION_ID + 2, TYPE, AGGREGATE);
        AggregateSnapshot aggregateSnapshot3 = createSnapshot(STREAM_ID, VERSION_ID + 3, TYPE, AGGREGATE);
        AggregateSnapshot aggregateSnapshot4 = createSnapshot(STREAM_ID, VERSION_ID + 4, TYPE, AGGREGATE);
        AggregateSnapshot aggregateSnapshot5 = createSnapshot(STREAM_ID, VERSION_ID + 5, TYPE, AGGREGATE);

        jdbcRepository.storeSnapshot(aggregateSnapshot1);
        jdbcRepository.storeSnapshot(aggregateSnapshot2);
        jdbcRepository.storeSnapshot(aggregateSnapshot3);
        jdbcRepository.storeSnapshot(aggregateSnapshot4);
        jdbcRepository.storeSnapshot(aggregateSnapshot5);

        Optional<AggregateSnapshot> snapshot = jdbcRepository.getLatestSnapshot(STREAM_ID);

        assertThat(snapshot, notNullValue());
        assertThat(snapshot, is(Optional.of(aggregateSnapshot5)));
    }

    @Test
    public void shouldRetrieveEarlierSnapshot() throws DuplicateSnapshotException, InvalidSequenceIdException {

        AggregateSnapshot aggregateSnapshot1 = createSnapshot(STREAM_ID, VERSION_ID + 1, TYPE, AGGREGATE);
        AggregateSnapshot aggregateSnapshot2 = createSnapshot(STREAM_ID, VERSION_ID + 2, TYPE, AGGREGATE);
        AggregateSnapshot aggregateSnapshot3 = createSnapshot(STREAM_ID, VERSION_ID + 3, TYPE, AGGREGATE);
        AggregateSnapshot aggregateSnapshot4 = createSnapshot(STREAM_ID, VERSION_ID + 4, TYPE, AGGREGATE);
        AggregateSnapshot aggregateSnapshot5 = createSnapshot(STREAM_ID, VERSION_ID + 6, TYPE, AGGREGATE);

        jdbcRepository.storeSnapshot(aggregateSnapshot1);
        jdbcRepository.storeSnapshot(aggregateSnapshot2);
        jdbcRepository.storeSnapshot(aggregateSnapshot3);
        jdbcRepository.storeSnapshot(aggregateSnapshot4);
        jdbcRepository.storeSnapshot(aggregateSnapshot5);

        Optional<AggregateSnapshot> snapshot = jdbcRepository.getEarlierSnapshot(STREAM_ID,11);

        assertThat(snapshot, notNullValue());
        assertThat(snapshot, is(Optional.of(aggregateSnapshot4)));
    }


    @Test
    public void shouldReturnOptionalNullIfNoSnapshotAvailable() throws InvalidSequenceIdException {

        Optional<AggregateSnapshot> snapshot = jdbcRepository.getLatestSnapshot(STREAM_ID);

        assertThat(snapshot.isPresent(), is(false));
    }

    @Test
    public void shouldThrowDuplicateSnapshotException() throws DuplicateSnapshotException, InvalidSequenceIdException {
        expectedException.expect(isA(JdbcRepositoryException.class));
        expectedException.expectMessage(containsString("Exception while storing sequence"));

        AggregateSnapshot aggregateSnapshot = createSnapshot(STREAM_ID, VERSION_ID + 1, TYPE, AGGREGATE);

        jdbcRepository.storeSnapshot(aggregateSnapshot);
        jdbcRepository.storeSnapshot(aggregateSnapshot);

        jdbcRepository.getLatestSnapshot(STREAM_ID);
    }

    @Test
    public void shouldThrowInvalidSequenceIdException() throws DuplicateSnapshotException, InvalidSequenceIdException {
        expectedException.expect(InvalidSequenceIdException.class);
        expectedException.expectMessage("Version is null for stream " + STREAM_ID);

        AggregateSnapshot aggregateSnapshot1 = createSnapshot(STREAM_ID, null, TYPE, AGGREGATE);

        jdbcRepository.storeSnapshot(aggregateSnapshot1);
    }

    private <T extends Aggregate> AggregateSnapshot createSnapshot(final UUID streamId, final Long sequenceId, Class<T> type, byte[] aggregate) {

        return new AggregateSnapshot(streamId, sequenceId, type, aggregate);
    }
}


