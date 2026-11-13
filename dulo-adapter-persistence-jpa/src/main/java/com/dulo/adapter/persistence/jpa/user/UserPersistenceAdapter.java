package com.dulo.adapter.persistence.jpa.user;

import com.dulo.core.access.port.out.UserRepository;
import com.dulo.domain.access.RoleKey;
import com.dulo.domain.shared.ConcurrentModificationException;
import com.dulo.domain.user.ExternalIdentity;
import com.dulo.domain.user.User;
import com.dulo.domain.user.UserId;
import com.dulo.domain.user.UserStatus;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository repository;

    UserPersistenceAdapter(final UserJpaRepository repository) {
        this.repository = repository;
    }

    private static void checkVersion(final User user, final long actual) {
        if (user.version() != actual) {
            throw new ConcurrentModificationException("User", user.id(), user.version(), actual);
        }
    }

    private static Set<RoleKey> roles(final UserJpaEntity entity, final String source) {
        return entity.getRoles().stream()
                .filter(assignment -> assignment.source().equals(source))
                .map(assignment -> new RoleKey(assignment.roleKey()))
                .collect(Collectors.toSet());
    }

    private static Set<UserRoleAssignment> assignments(final User user) {
        final Set<UserRoleAssignment> assignments = new HashSet<>();
        user.claimRoles()
                .forEach(role ->
                        assignments.add(
                                new UserRoleAssignment(role.value(), UserRoleAssignment.CLAIM)));
        user.manualRoles()
                .forEach(role ->
                        assignments.add(
                                new UserRoleAssignment(role.value(), UserRoleAssignment.MANUAL)));
        return assignments;
    }

    private static User toDomain(final UserJpaEntity entity) {
        return new User(
                UserId.parse(entity.getId()),
                new ExternalIdentity(entity.getProviderId(), entity.getExternalSubject()),
                entity.getEmail(),
                entity.getDisplayName(),
                UserStatus.valueOf(entity.getStatus().toUpperCase(Locale.ROOT)),
                entity.isRoot(),
                roles(entity, UserRoleAssignment.CLAIM),
                roles(entity, UserRoleAssignment.MANUAL),
                entity.getLastSignIn(),
                entity.getVersion());
    }

    @Override
    public Optional<User> findById(final UserId id) {
        return this.repository.findById(id.toString()).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<User> findByExternalIdentity(final ExternalIdentity identity) {
        return this.repository
                .findByProviderIdAndExternalSubject(identity.providerId(), identity.subject())
                .map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public User save(final User user) {
        final String id = user.id().toString();
        final UserJpaEntity entity =
                this.repository
                        .findById(id)
                        .map(existing -> {
                            checkVersion(user, existing.getVersion());
                            return existing;
                        })
                        .orElseGet(() -> {
                            checkVersion(user, 0L);
                            return new UserJpaEntity(id);
                        });

        entity.setProviderId(user.externalIdentity().providerId());
        entity.setExternalSubject(user.externalIdentity().subject());
        entity.setEmail(user.email());
        entity.setDisplayName(user.displayName());
        entity.setStatus(user.status().name().toLowerCase(Locale.ROOT));
        entity.setRoot(user.root());
        entity.setRoles(assignments(user));
        entity.setLastSignIn(user.lastSignIn());
        return toDomain(this.repository.saveAndFlush(entity));
    }
}
