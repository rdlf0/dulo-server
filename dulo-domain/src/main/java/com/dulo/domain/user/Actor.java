package com.dulo.domain.user;

import com.dulo.domain.access.Permissions;

import java.util.Objects;

/**
 * A signed-in, active user, with the permissions resolved for the current request.
 *
 * @param permissions the union of the user's role permissions; everything for the root user
 */
public record Actor(UserId id, boolean root, Permissions permissions) {

    public Actor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(permissions, "permissions");
    }
}
