package com.dulo.adapter.persistence.jpa.role;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Row of the {@code dulo_role} table. Hibernate creates it through the no-argument constructor and
 * fills the fields afterwards, so fields without a value before that are {@code @Nullable} and
 * their getters check them.
 */
@Entity
@Table(name = "dulo_role")
class RoleJpaEntity {

    @Id
    @Column(name = "role_key", length = 50)
    private @Nullable String roleKey;

    @Column(nullable = false, length = 100)
    private @Nullable String name;

    @Column(name = "built_in", nullable = false)
    private boolean builtIn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permission", joinColumns = @JoinColumn(name = "role_key"))
    // Not final: JPA forbids final persistent fields, and Hibernate replaces the set on load.
    @SuppressWarnings("FieldMayBeFinal")
    private Set<PermissionGrant> permissions = new HashSet<>();

    @Version private @Nullable Long version;

    protected RoleJpaEntity() {}

    RoleJpaEntity(final String roleKey) {
        this.roleKey = roleKey;
    }

    String getRoleKey() {
        return Objects.requireNonNull(roleKey, "roleKey");
    }

    String getName() {
        return Objects.requireNonNull(name, "name");
    }

    void setName(final String name) {
        this.name = name;
    }

    boolean isBuiltIn() {
        return builtIn;
    }

    void setBuiltIn(final boolean builtIn) {
        this.builtIn = builtIn;
    }

    Set<PermissionGrant> getPermissions() {
        return permissions;
    }

    void setPermissions(final Set<PermissionGrant> permissions) {
        // Changes the managed set in place, so Hibernate only writes the differences.
        this.permissions.retainAll(permissions);
        this.permissions.addAll(permissions);
    }

    /** Version of a stored entity. Only a new entity that was never flushed has none. */
    long getVersion() {
        return Objects.requireNonNull(version, "version");
    }
}
