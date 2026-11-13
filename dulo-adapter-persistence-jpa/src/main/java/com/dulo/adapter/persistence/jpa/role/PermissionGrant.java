package com.dulo.adapter.persistence.jpa.role;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Row of the {@code role_permission} table: one access on one feature. */
@Embeddable
record PermissionGrant(
        @Column(name = "feature", length = 50, nullable = false) String feature,
        @Column(name = "access", length = 20, nullable = false) String access) {}
