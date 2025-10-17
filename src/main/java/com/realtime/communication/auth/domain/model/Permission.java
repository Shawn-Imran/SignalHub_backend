package com.realtime.communication.auth.domain.model;

import java.util.UUID;

/**
 * Permission entity
 * Represents specific permissions that can be granted to roles
 */
public class Permission {
    private final UUID id;
    private String name;
    private String description;

    public Permission(UUID id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
package com.realtime.communication.auth.domain.model;

import java.util.UUID;

/**
 * Value object representing a User ID
 */
public record UserId(UUID value) {
    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }
}

