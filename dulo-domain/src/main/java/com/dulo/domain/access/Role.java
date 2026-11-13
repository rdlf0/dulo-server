package com.dulo.domain.access;

import java.util.Objects;

/**
 * A named set of permissions that users are given.
 *
 * @param builtIn true for the roles the initial setup creates; only the root user may change them
 * @param version optimistic-locking version, 0 for a role that has not been stored yet
 */
public record Role(
        RoleKey key, String name, boolean builtIn, Permissions permissions, long version) {

    public static final int NAME_MAX_LENGTH = 100;

    public Role {
        Objects.requireNonNull(key, "key");
        name = Objects.requireNonNull(name, "name").strip();
        if (name.isEmpty() || name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Role name must be 1-%d characters".formatted(NAME_MAX_LENGTH));
        }
        Objects.requireNonNull(permissions, "permissions");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
