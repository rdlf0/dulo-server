package com.dulo.core.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dulo.core.identity.port.in.ExternalSignIn;
import com.dulo.core.identity.port.out.IdentityProvidersPort;
import com.dulo.core.testing.DirectUnitOfWork;
import com.dulo.core.testing.InMemoryUsers;
import com.dulo.domain.access.RoleKey;
import com.dulo.domain.identity.RegistrationMode;
import com.dulo.domain.identity.RoleMapping;
import com.dulo.domain.user.ExternalIdentity;
import com.dulo.domain.user.User;
import com.dulo.domain.user.UserId;
import com.dulo.domain.user.UserStatus;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class SignInServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");
    private static final RoleKey ADMIN = new RoleKey("admin");
    private static final RoleKey VIEWER = new RoleKey("viewer");
    private static final ExternalIdentity ADA = new ExternalIdentity("pocketid", "ada");

    private final InMemoryUsers users = new InMemoryUsers();
    private RegistrationMode mode = RegistrationMode.APPROVAL;
    private RoleMapping mapping = new RoleMapping(Map.of("family-admins", Set.of(ADMIN)), VIEWER);
    private final SignInService service =
            new SignInService(
                    this.users,
                    new IdentityProvidersPort() {
                        @Override
                        public Optional<RoleMapping> roleMapping(final String providerId) {
                            return providerId.equals("pocketid")
                                    ? Optional.of(SignInServiceTest.this.mapping)
                                    : Optional.empty();
                        }

                        @Override
                        public RegistrationMode registrationMode() {
                            return SignInServiceTest.this.mode;
                        }
                    },
                    new DirectUnitOfWork(),
                    Clock.fixed(NOW, ZoneOffset.UTC));

    private static ExternalSignIn signIn(final String... groups) {
        return new ExternalSignIn(ADA, "ada@example.com", "Ada", Set.of(groups));
    }

    @Test
    void newUserWithoutMatchingClaimsWaitsForApproval() {
        final User user = this.signInAndLoad(signIn("other"));

        assertThat(user.status()).isEqualTo(UserStatus.PENDING);
        assertThat(user.claimRoles()).containsExactly(VIEWER);
        assertThat(user.email()).isEqualTo("ada@example.com");
        assertThat(user.lastSignIn()).isEqualTo(NOW);
    }

    @Test
    void newUserIsActiveInAutoMode() {
        this.mode = RegistrationMode.AUTO;

        assertThat(this.signInAndLoad(signIn()).status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void matchingClaimsActivateANewUser() {
        final User user = this.signInAndLoad(signIn("family-admins"));

        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.claimRoles()).containsExactly(ADMIN);
    }

    @Test
    void noDefaultRoleMeansNoRoles() {
        this.mapping = RoleMapping.none();

        assertThat(this.signInAndLoad(signIn()).claimRoles()).isEmpty();
    }

    @Test
    void signingInAgainUpdatesTheSameUser() {
        final UserId first = this.service.signIn(signIn());

        final UserId second =
                this.service.signIn(new ExternalSignIn(ADA, null, "Ada L.", Set.of("other")));

        assertThat(second).isEqualTo(first);
        assertThat(this.users.size()).isEqualTo(1);
        final User user = this.users.findById(first).orElseThrow();
        assertThat(user.displayName()).isEqualTo("Ada L.");
        assertThat(user.email()).isNull();
        assertThat(user.version()).isEqualTo(1);
    }

    @Test
    void laterMatchingClaimsApproveAPendingUser() {
        final UserId id = this.service.signIn(signIn());

        this.service.signIn(signIn("family-admins"));

        final User user = this.users.findById(id).orElseThrow();
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.claimRoles()).containsExactly(ADMIN);
    }

    @Test
    void signInKeepsADisabledUserDisabled() {
        final UserId id = this.service.signIn(signIn());
        this.users.save(this.users.findById(id).orElseThrow().withStatus(UserStatus.DISABLED));

        this.service.signIn(signIn("family-admins"));

        assertThat(this.users.findById(id).orElseThrow().status()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void rejectsUnknownProviders() {
        final ExternalSignIn unknown =
                new ExternalSignIn(new ExternalIdentity("ghost", "ada"), null, null, Set.of());

        assertThatThrownBy(() -> this.service.signIn(unknown))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost");
    }

    private User signInAndLoad(final ExternalSignIn signIn) {
        return this.users.findById(this.service.signIn(signIn)).orElseThrow();
    }
}
