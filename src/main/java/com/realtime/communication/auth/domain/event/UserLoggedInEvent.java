package com.realtime.communication.auth.domain.event;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.shared.application.event.Event;
import lombok.Getter;

/**
 * Domain event triggered when a user successfully logs in.
 * This event can be consumed by other services for tracking user activity,
 * updating presence status, triggering security alerts, etc.
 */
@Getter
public class UserLoggedInEvent extends Event {

    private final String userId;
    private final String username;
    private final String deviceInfo;
    private final String ipAddress;

    public UserLoggedInEvent(UserId userId, String username, String deviceInfo, String ipAddress) {
        super();
        this.userId = userId.getValue().toString();
        this.username = username;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
    }
}

