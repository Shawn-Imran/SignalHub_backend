package com.realtime.communication.shared.application.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events.
 * Events represent things that have happened in the domain.
 */
@Getter
public abstract class Event {

    private final String eventId;
    private final Instant occurredOn;
    private final String eventType;

    protected Event() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
        this.eventType = this.getClass().getSimpleName();
    }
}
