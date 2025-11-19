package com.realtime.communication.auth.domain.event;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.shared.application.event.Event;
import lombok.Getter;

/**
 * Domain event triggered when a user successfully registers.
 * This event can be consumed by other services to perform additional actions
 * such as sending welcome emails, creating initial preferences, etc.
 */
@Getter
public class UserRegisteredEvent extends Event {

    private final String userId;
    private final String username;
    private final String email;

    public UserRegisteredEvent(UserId userId, String username, String email) {
        super();
        this.userId = userId.getValue().toString();
        this.username = username;
        this.email = email;
    }
}

