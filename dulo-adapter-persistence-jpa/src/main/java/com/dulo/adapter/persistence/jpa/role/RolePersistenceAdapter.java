package com.dulo.adapter.persistence.jpa.role;

import com.dulo.core.access.port.out.RoleRepository;
import com.dulo.domain.access.Access;
import com.dulo.domain.access.Permissions;
import com.dulo.domain.access.Role;
import com.dulo.domain.access.RoleKey;
import com.dulo.domain.feature.Feature;
import com.dulo.domain.shared.ConcurrentModificationException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RolePersistenceAdapter implements RoleRepository {

    private final RoleJpaRepository repository;

    RolePersistenceAdapter(final RoleJpaRepository repository) {
        this.repository = repository;
    }

    private static void checkVersion(final Role role, final long actual) {
        if (role.version() != actual) {
            throw new ConcurrentModificationException("Role", role.key(), role.version(), actual);
        }
    }

    private static Permissions permissions(final Set<PermissionGrant> grants) {
        final Map<Feature, Set<Access>> map = new EnumMap<>(Feature.class);
        grants.forEach(grant ->
                // Grants of features removed in newer versions are ignored.
                Feature.fromKey(grant.feature())
                        .ifPresent(feature ->
                                map.computeIfAbsent(feature, _ -> EnumSet.noneOf(Access.class))
                                        .add(
                                                Access.valueOf(
                                                        grant.access().toUpperCase(Locale.ROOT)))));
        return new Permissions(map);
    }

    private static Set<PermissionGrant> grants(final Permissions permissions) {
        final Set<PermissionGrant> grants = new HashSet<>();
        permissions
                .grants()
                .forEach((feature, access) ->
                        access.forEach(
                                a -> grants.add(new PermissionGrant(feature.key(), a.key()))));
        return grants;
    }

    private static Role toDomain(final RoleJpaEntity entity) {
        return new Role(
                new RoleKey(entity.getRoleKey()),
                entity.getName(),
                entity.isBuiltIn(),
                permissions(entity.getPermissions()),
                entity.getVersion());
    }

    @Override
    public List<Role> findAll() {
        return this.repository.findAllByOrderByRoleKey().stream()
                .map(RolePersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Role> findAllByKeys(final Set<RoleKey> keys) {
        if (keys.isEmpty()) {
            return List.of();
        }
        return this.repository
                .findAllByRoleKeyInOrderByRoleKey(keys.stream().map(RoleKey::value).toList())
                .stream()
                .map(RolePersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<Role> find(final RoleKey key) {
        return this.repository.findById(key.value()).map(RolePersistenceAdapter::toDomain);
    }

    @Override
    public Role save(final Role role) {
        final String key = role.key().value();
        final RoleJpaEntity entity =
                this.repository
                        .findById(key)
                        .map(existing -> {
                            checkVersion(role, existing.getVersion());
                            return existing;
                        })
                        .orElseGet(() -> {
                            checkVersion(role, 0L);
                            return new RoleJpaEntity(key);
                        });

        entity.setName(role.name());
        entity.setBuiltIn(role.builtIn());
        entity.setPermissions(grants(role.permissions()));
        return toDomain(this.repository.saveAndFlush(entity));
    }
}
