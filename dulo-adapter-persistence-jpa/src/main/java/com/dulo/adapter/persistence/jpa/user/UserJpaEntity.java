package com.dulo.adapter.persistence.jpa.user;

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

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Row of the {@code dulo_user} table. Hibernate creates it through the no-argument constructor and
 * fills the fields afterwards, so fields without a value before that are {@code @Nullable} and
 * their getters check them.
 */
@Entity
@Table(name = "dulo_user")
class UserJpaEntity {

    @Id
    @Column(length = 36)
    private @Nullable String id;

    @Column(name = "provider_id", nullable = false, length = 50)
    private @Nullable String providerId;

    @Column(name = "external_subject", nullable = false, length = 255)
    private @Nullable String externalSubject;

    @Column(length = 320)
    private @Nullable String email;

    @Column(name = "display_name", length = 200)
    private @Nullable String displayName;

    @Column(nullable = false, length = 20)
    private @Nullable String status;

    @Column(name = "is_root", nullable = false)
    private boolean root;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
    // Not final: JPA forbids final persistent fields, and Hibernate replaces the set on load.
    @SuppressWarnings("FieldMayBeFinal")
    private Set<UserRoleAssignment> roles = new HashSet<>();

    @Column(name = "last_sign_in", nullable = false)
    private @Nullable Instant lastSignIn;

    @Version private @Nullable Long version;

    protected UserJpaEntity() {}

    UserJpaEntity(final String id) {
        this.id = id;
    }

    String getId() {
        return Objects.requireNonNull(id, "id");
    }

    String getProviderId() {
        return Objects.requireNonNull(providerId, "providerId");
    }

    void setProviderId(final String providerId) {
        this.providerId = providerId;
    }

    String getExternalSubject() {
        return Objects.requireNonNull(externalSubject, "externalSubject");
    }

    void setExternalSubject(final String externalSubject) {
        this.externalSubject = externalSubject;
    }

    @Nullable String getEmail() {
        return email;
    }

    void setEmail(final @Nullable String email) {
        this.email = email;
    }

    @Nullable String getDisplayName() {
        return displayName;
    }

    void setDisplayName(final @Nullable String displayName) {
        this.displayName = displayName;
    }

    String getStatus() {
        return Objects.requireNonNull(status, "status");
    }

    void setStatus(final String status) {
        this.status = status;
    }

    boolean isRoot() {
        return root;
    }

    void setRoot(final boolean root) {
        this.root = root;
    }

    Set<UserRoleAssignment> getRoles() {
        return roles;
    }

    void setRoles(final Set<UserRoleAssignment> roles) {
        // Changes the managed set in place, so Hibernate only writes the differences.
        this.roles.retainAll(roles);
        this.roles.addAll(roles);
    }

    Instant getLastSignIn() {
        return Objects.requireNonNull(lastSignIn, "lastSignIn");
    }

    void setLastSignIn(final Instant lastSignIn) {
        this.lastSignIn = lastSignIn;
    }

    /** Version of a stored entity. Only a new entity that was never flushed has none. */
    long getVersion() {
        return Objects.requireNonNull(version, "version");
    }
}
