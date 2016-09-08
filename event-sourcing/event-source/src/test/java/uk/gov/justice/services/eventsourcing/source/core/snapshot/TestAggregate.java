package uk.gov.justice.services.eventsourcing.source.core.snapshot;

import uk.gov.justice.domain.aggregate.Aggregate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TestAggregate implements Aggregate, Serializable {

    private static final long serialVersionUID = 42L;

    public String state;
    private int eventCount;
    public List<Object> recordedEvents = new ArrayList<>();

    public TestAggregate() {

    }

    public TestAggregate(String state) {
        this.state = state;
    }

    @Override
    public Object apply(Object event) {
        eventCount++;
        recordedEvents.add(event);
        return event;
    }
}