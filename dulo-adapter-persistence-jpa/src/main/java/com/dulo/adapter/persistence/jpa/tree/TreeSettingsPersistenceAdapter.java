package com.dulo.adapter.persistence.jpa.tree;

import com.dulo.core.tree.port.out.TreeSettingsRepository;
import com.dulo.domain.feature.Feature;
import com.dulo.domain.shared.ConcurrentModificationException;
import com.dulo.domain.tree.TreeId;
import com.dulo.domain.tree.TreeSettings;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class TreeSettingsPersistenceAdapter implements TreeSettingsRepository {

    private final TreeSettingsJpaRepository repository;

    TreeSettingsPersistenceAdapter(final TreeSettingsJpaRepository repository) {
        this.repository = repository;
    }

    private static void checkVersion(final TreeSettings tree, final long actual) {
        if (tree.version() != actual) {
            throw new ConcurrentModificationException("Tree", tree.id(), tree.version(), actual);
        }
    }

    private static TreeSettings toDomain(final TreeSettingsJpaEntity entity) {
        return new TreeSettings(
                new TreeId(UUID.fromString(entity.getId())),
                entity.getName(),
                entity.getDescription(),
                entity.getEnabledFeatures().stream()
                        // Keys of features removed in newer versions are ignored.
                        .flatMap(key -> Feature.fromKey(key).stream())
                        .collect(Collectors.toSet()),
                entity.getVersion());
    }

    @Override
    public Optional<TreeSettings> find() {
        return repository.findAll().stream()
                .findFirst()
                .map(TreeSettingsPersistenceAdapter::toDomain);
    }

    @Override
    public TreeSettings save(final TreeSettings tree) {
        final String id = tree.id().toString();
        final TreeSettingsJpaEntity entity =
                repository
                        .findById(id)
                        .map(existing -> {
                            checkVersion(tree, existing.getVersion());
                            return existing;
                        })
                        .orElseGet(() -> {
                            checkVersion(tree, 0L);
                            return new TreeSettingsJpaEntity(id);
                        });

        entity.setName(tree.name());
        entity.setDescription(tree.description());
        entity.setEnabledFeatures(
                tree.enabledFeatures().stream().map(Feature::key).collect(Collectors.toSet()));
        return toDomain(repository.saveAndFlush(entity));
    }
}
