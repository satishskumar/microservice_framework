package uk.gov.justice.domain.snapshot;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.UUID;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;


public class AggregateSnapshotTest {

    @Test
    public void shouldCreateAnAggregateSnapshot() throws Exception {
        final UUID streamID = UUID.randomUUID();
        final long versionId = 1l;
        final Class<? extends Aggregate> aggregateClass = TestAggregate.class;
        final TestAggregate aggregate = new TestAggregate("STATE1");

        AggregateSnapshot snapshot = new AggregateSnapshot(streamID, versionId, aggregateClass, aggregate);

        assertThat(snapshot.getStreamId(), is(streamID));
        assertThat(snapshot.getVersionId(), is(versionId));
        assertThat(snapshot.getType(), equalTo(TestAggregate.class));
        assertThat(snapshot.getAggregateByteRepresentation(), is(SerializationUtils.serialize(aggregate)));

    }

    @Test
    public void shouldGetAnAggregateSnapshot() throws Exception {
        final UUID streamID = UUID.randomUUID();
        final long versionId = 1l;
        final Class<? extends Aggregate> aggregateClass = TestAggregate.class;
        final TestAggregate aggregate = new TestAggregate("STATE1");

        AggregateSnapshot snapshot = new AggregateSnapshot(streamID, versionId, aggregateClass, aggregate);

        assertThat(snapshot.getStreamId(), is(streamID));
        assertThat(snapshot.getVersionId(), is(versionId));
        assertThat(snapshot.getType(), equalTo(TestAggregate.class));
        assertThat(snapshot.getAggregate(), is(aggregate));
    }


    public static class TestAggregate implements Aggregate {
        private static final long serialVersionUID = 42L;

        private final String name;

        public TestAggregate(String name) {
            this.name = name;
        }

        @Override
        public Object apply(Object event) {
            return event;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestAggregate that = (TestAggregate) o;

            return name != null ? name.equals(that.name) : that.name == null;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }
}