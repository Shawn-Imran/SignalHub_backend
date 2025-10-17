package com.realtime.communication.auth.domain.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Role entity
 * Represents user roles and permissions
 */
public class Role {
    private final UUID id;
    private String name;
    private Set<Permission> permissions;

    public Role(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.permissions = new HashSet<>();
    }

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Permission> getPermissions() {
        return new HashSet<>(permissions);
    }
}

