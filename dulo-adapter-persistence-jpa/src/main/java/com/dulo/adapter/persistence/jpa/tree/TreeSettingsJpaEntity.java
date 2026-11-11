package com.dulo.adapter.persistence.jpa.tree;

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
 * Row of the {@code tree_settings} table. Hibernate creates it through the no-argument constructor
 * and fills the fields afterwards, so fields without a value before that are {@code @Nullable} and
 * their getters check them.
 */
@Entity
@Table(name = "tree_settings")
class TreeSettingsJpaEntity {

    @Id
    @Column(length = 36)
    private @Nullable String id;

    @Column(nullable = false, length = 200)
    private @Nullable String name;

    @Column(length = 2000)
    private @Nullable String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tree_feature", joinColumns = @JoinColumn(name = "tree_id"))
    @Column(name = "feature", length = 50, nullable = false)
    // Not final: JPA forbids final persistent fields, and Hibernate replaces the set on load.
    @SuppressWarnings("FieldMayBeFinal")
    private Set<String> enabledFeatures = new HashSet<>();

    @Version private @Nullable Long version;

    protected TreeSettingsJpaEntity() {}

    TreeSettingsJpaEntity(final String id) {
        this.id = id;
    }

    String getId() {
        return Objects.requireNonNull(id, "id");
    }

    String getName() {
        return Objects.requireNonNull(name, "name");
    }

    void setName(final String name) {
        this.name = name;
    }

    @Nullable String getDescription() {
        return description;
    }

    void setDescription(final @Nullable String description) {
        this.description = description;
    }

    Set<String> getEnabledFeatures() {
        return enabledFeatures;
    }

    void setEnabledFeatures(final Set<String> enabledFeatures) {
        this.enabledFeatures.clear();
        this.enabledFeatures.addAll(enabledFeatures);
    }

    /** Version of a stored entity. Only a new entity that was never flushed has none. */
    long getVersion() {
        return Objects.requireNonNull(version, "version");
    }
}
