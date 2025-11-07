package com.realtime.communication.auth.domain.model;

import lombok.Getter;

import java.util.UUID;

/**
 * Permission entity
 * Represents specific permissions that can be granted to roles
 */
@Getter
public class Permission {
    private final UUID id;
    private final String name;
    private final String description;

    public Permission(UUID id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
