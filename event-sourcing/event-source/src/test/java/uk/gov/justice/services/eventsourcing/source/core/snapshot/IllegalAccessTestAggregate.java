package uk.gov.justice.services.eventsourcing.source.core.snapshot;

import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.stream.Stream;

public class IllegalAccessTestAggregate implements Aggregate {

    private IllegalAccessTestAggregate() {

    }

    @Override
    public Object apply(Object event) {
        return null;
    }

    @Override
    public Stream<Object> apply(Stream<Object> events) {
        return null;
    }
}
