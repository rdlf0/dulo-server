package com.dulo.adapter.persistence.jpa.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * Row of the {@code signing_key} table. Keys are never updated. Hibernate creates the entity
 * through the no-argument constructor and fills the fields afterwards, so the fields are
 * {@code @Nullable} and their getters check them.
 */
@Entity
@Table(name = "signing_key")
class SigningKeyJpaEntity {

    @Id
    @Column(length = 64)
    private @Nullable String id;

    @Column(nullable = false)
    private @Nullable String material;

    @Column(name = "created_at", nullable = false)
    private @Nullable Instant createdAt;

    protected SigningKeyJpaEntity() {}

    SigningKeyJpaEntity(final String id, final String material, final Instant createdAt) {
        this.id = id;
        this.material = material;
        this.createdAt = createdAt;
    }

    String getId() {
        return Objects.requireNonNull(id, "id");
    }

    String getMaterial() {
        return Objects.requireNonNull(material, "material");
    }

    Instant getCreatedAt() {
        return Objects.requireNonNull(createdAt, "createdAt");
    }
}
