package com.dulo.adapter.persistence.jpa.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Row of the {@code user_role} table.
 *
 * @param source {@code claim} for roles mapped from the provider's claims, {@code manual} for roles
 *     an administrator assigned
 */
@Embeddable
record UserRoleAssignment(
        @Column(name = "role_key", length = 50, nullable = false) String roleKey,
        @Column(name = "source", length = 10, nullable = false) String source) {

    static final String CLAIM = "claim";
    static final String MANUAL = "manual";
}
