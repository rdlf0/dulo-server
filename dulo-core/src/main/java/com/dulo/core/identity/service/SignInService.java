package com.dulo.core.identity.service;

import com.dulo.core.access.port.out.UserRepository;
import com.dulo.core.identity.port.in.ExternalSignIn;
import com.dulo.core.identity.port.in.SignInUseCase;
import com.dulo.core.identity.port.out.IdentityProvidersPort;
import com.dulo.core.shared.port.out.UnitOfWork;
import com.dulo.domain.access.RoleKey;
import com.dulo.domain.identity.RegistrationMode;
import com.dulo.domain.identity.RoleMapping;
import com.dulo.domain.user.User;
import com.dulo.domain.user.UserId;
import com.dulo.domain.user.UserStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

public class SignInService implements SignInUseCase {

    private final UserRepository users;
    private final IdentityProvidersPort providers;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public SignInService(
            final UserRepository users,
            final IdentityProvidersPort providers,
            final UnitOfWork unitOfWork,
            final Clock clock) {
        this.users = users;
        this.providers = providers;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public UserId signIn(final ExternalSignIn signIn) {
        final String providerId = signIn.identity().providerId();
        final RoleMapping mapping =
                this.providers
                        .roleMapping(providerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Unknown identity provider '%s'".formatted(providerId)));
        final Set<RoleKey> mapped = mapping.mappedRoles(signIn.roleClaimValues());
        final Set<RoleKey> claimRoles =
                mapped.isEmpty()
                        ? mapping.defaultRoleIfAny().map(Set::of).orElse(Set.of())
                        : mapped;
        final Instant now = this.clock.instant();

        return this.unitOfWork.write(() -> {
            final User user =
                    this.users
                            .findByExternalIdentity(signIn.identity())
                            .map(existing -> {
                                final User updated =
                                        existing.signedIn(
                                                signIn.email(),
                                                signIn.displayName(),
                                                claimRoles,
                                                now);
                                // A matching mapping approves a pending user.
                                return updated.status() == UserStatus.PENDING && !mapped.isEmpty()
                                        ? updated.withStatus(UserStatus.ACTIVE)
                                        : updated;
                            })
                            .orElseGet(() ->
                                    User.register(
                                            signIn.identity(),
                                            signIn.email(),
                                            signIn.displayName(),
                                            this.initialStatus(mapped),
                                            claimRoles,
                                            now));
            return this.users.save(user).id();
        });
    }

    private UserStatus initialStatus(final Set<RoleKey> mappedRoles) {
        return this.providers.registrationMode() == RegistrationMode.AUTO || !mappedRoles.isEmpty()
                ? UserStatus.ACTIVE
                : UserStatus.PENDING;
    }
}
