package com.dulo.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dulo.domain.access.RoleKey;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

class UserTest {

    private static final ExternalIdentity IDENTITY = new ExternalIdentity("pocketid", "abc-123");
    private static final Instant FIRST = Instant.parse("2026-11-13T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-14T10:00:00Z");
    private static final RoleKey EDITOR = new RoleKey("editor");
    private static final RoleKey VIEWER = new RoleKey("viewer");

    private static User pending() {
        return User.register(
                IDENTITY, "ada@example.com", "Ada", UserStatus.PENDING, Set.of(VIEWER), FIRST);
    }

    @Test
    void registersANewUser() {
        final User user = pending();

        assertThat(user.version()).isZero();
        assertThat(user.root()).isFalse();
        assertThat(user.claimRoles()).containsExactly(VIEWER);
        assertThat(user.manualRoles()).isEmpty();
        assertThat(user.lastSignIn()).isEqualTo(FIRST);
    }

    @Test
    void rolesAreClaimAndManualRoles() {
        final User user = pending().withManualRoles(Set.of(EDITOR));

        assertThat(user.roles()).containsExactlyInAnyOrder(VIEWER, EDITOR);
    }

    @Test
    void signInReplacesProfileAndClaimRolesButKeepsTheRest() {
        final User user =
                pending()
                        .withManualRoles(Set.of(EDITOR))
                        .signedIn("ada@example.org", "Ada L.", Set.of(), LATER);

        assertThat(user.email()).isEqualTo("ada@example.org");
        assertThat(user.displayName()).isEqualTo("Ada L.");
        assertThat(user.claimRoles()).isEmpty();
        assertThat(user.manualRoles()).containsExactly(EDITOR);
        assertThat(user.status()).isEqualTo(UserStatus.PENDING);
        assertThat(user.lastSignIn()).isEqualTo(LATER);
    }

    @Test
    void blankProfileValuesBecomeNullAndLongOnesAreCut() {
        final User user =
                User.register(IDENTITY, "  ", "x".repeat(300), UserStatus.ACTIVE, Set.of(), FIRST);

        assertThat(user.email()).isNull();
        assertThat(user.displayName()).hasSize(User.DISPLAY_NAME_MAX_LENGTH);
    }

    @Test
    void rootIsActive() {
        final User root = pending().asRoot();

        assertThat(root.root()).isTrue();
        assertThat(root.status()).isEqualTo(UserStatus.ACTIVE);
        assertThatThrownBy(() -> root.withStatus(UserStatus.DISABLED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeVersions() {
        final User user = pending();

        assertThatThrownBy(() ->
                        new User(
                                user.id(),
                                IDENTITY,
                                null,
                                null,
                                UserStatus.ACTIVE,
                                false,
                                Set.of(),
                                Set.of(),
                                FIRST,
                                -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesTheExternalIdentity() {
        assertThatThrownBy(() -> new ExternalIdentity("", "abc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExternalIdentity("google", "x".repeat(256)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userIdsRoundTripThroughStrings() {
        final UserId id = UserId.newId();

        assertThat(UserId.parse(id.toString())).isEqualTo(id);
        assertThatThrownBy(() -> UserId.parse("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
