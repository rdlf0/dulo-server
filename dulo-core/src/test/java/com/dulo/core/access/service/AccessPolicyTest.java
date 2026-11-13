package com.dulo.core.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dulo.core.testing.InMemoryRoles;
import com.dulo.core.testing.InMemoryTrees;
import com.dulo.core.testing.InMemoryUsers;
import com.dulo.domain.access.Access;
import com.dulo.domain.access.AccessDeniedException;
import com.dulo.domain.access.AccessDeniedException.Reason;
import com.dulo.domain.access.DefaultRoles;
import com.dulo.domain.access.Permissions;
import com.dulo.domain.access.RoleKey;
import com.dulo.domain.feature.Feature;
import com.dulo.domain.feature.FeatureOverrides;
import com.dulo.domain.tree.TreeSettings;
import com.dulo.domain.user.Actor;
import com.dulo.domain.user.ExternalIdentity;
import com.dulo.domain.user.User;
import com.dulo.domain.user.UserId;
import com.dulo.domain.user.UserStatus;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

class AccessPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

    private final InMemoryUsers users = new InMemoryUsers();
    private final InMemoryTrees trees =
            new InMemoryTrees(
                    TreeSettings.create(
                            "Smiths", null, EnumSet.of(Feature.PEOPLE, Feature.PLACES)));
    private FeatureOverrides overrides = FeatureOverrides.none();
    private final AccessPolicy policy =
            new AccessPolicy(
                    this.users,
                    new InMemoryRoles(DefaultRoles.all()),
                    this.trees,
                    () -> this.overrides);

    private static void assertDenied(final Runnable check, final Reason reason) {
        assertThatThrownBy(check::run)
                .isInstanceOfSatisfying(
                        AccessDeniedException.class, e -> assertThat(e.reason()).isEqualTo(reason));
    }

    @Test
    void rolesGrantAccess() {
        final UserId viewer = this.user(UserStatus.ACTIVE, DefaultRoles.VIEWER);

        final Actor actor = this.policy.require(viewer, Feature.PEOPLE, Access.VIEW);

        assertThat(actor.id()).isEqualTo(viewer);
        assertThat(actor.root()).isFalse();
        assertDenied(
                () -> this.policy.require(viewer, Feature.PEOPLE, Access.EDIT),
                Reason.NOT_PERMITTED);
    }

    @Test
    void permissionsOfAllRolesAreCombined() {
        final UserId user =
                this.user(UserStatus.ACTIVE, DefaultRoles.VIEWER, DefaultRoles.CONTRIBUTOR);

        assertThat(this.policy.require(user, Feature.PEOPLE, Access.CREATE).permissions())
                .isEqualTo(
                        this.permissionsOf(DefaultRoles.VIEWER)
                                .union(this.permissionsOf(DefaultRoles.CONTRIBUTOR)));
    }

    @Test
    void unknownRolesGrantNothing() {
        final UserId user = this.user(UserStatus.ACTIVE, new RoleKey("ghost"));

        assertThat(this.policy.requireActive(user).permissions()).isEqualTo(Permissions.none());
    }

    @Test
    void rootBypassesRoles() {
        final UserId root = this.users.save(this.newUser(UserStatus.ACTIVE).asRoot()).id();

        final Actor actor = this.policy.require(root, Feature.ADMINISTRATION, Access.DELETE);

        assertThat(actor.root()).isTrue();
        assertThat(actor.permissions()).isEqualTo(Permissions.all());
    }

    @Test
    void disabledFeaturesAreDeniedEvenForRoot() {
        final UserId root = this.users.save(this.newUser(UserStatus.ACTIVE).asRoot()).id();

        assertDenied(
                () -> this.policy.require(root, Feature.MAP_VIEW, Access.VIEW),
                Reason.FEATURE_DISABLED);
    }

    @Test
    void configurationOverridesTheStoredFeatureState() {
        final UserId admin = this.user(UserStatus.ACTIVE, DefaultRoles.ADMIN);
        this.overrides =
                new FeatureOverrides(Map.of(Feature.PEOPLE, false, Feature.MAP_VIEW, true));

        assertDenied(
                () -> this.policy.require(admin, Feature.PEOPLE, Access.VIEW),
                Reason.FEATURE_DISABLED);
        assertThat(this.policy.require(admin, Feature.MAP_VIEW, Access.VIEW).id()).isEqualTo(admin);
    }

    @Test
    void beforeSetupOnlyForcedAndAlwaysOnFeaturesAreEnabled() {
        this.trees.clear();
        final UserId admin = this.user(UserStatus.ACTIVE, DefaultRoles.ADMIN);
        this.overrides = new FeatureOverrides(Map.of(Feature.TIMELINE, true));

        assertThat(this.policy.require(admin, Feature.ADMINISTRATION, Access.VIEW).id())
                .isEqualTo(admin);
        assertThat(this.policy.require(admin, Feature.TIMELINE, Access.VIEW).id()).isEqualTo(admin);
        assertDenied(
                () -> this.policy.require(admin, Feature.PEOPLE, Access.VIEW),
                Reason.FEATURE_DISABLED);
    }

    @Test
    void inactiveAndUnknownUsersAreDenied() {
        final UserId pending = this.user(UserStatus.PENDING, DefaultRoles.ADMIN);
        final UserId disabled = this.user(UserStatus.DISABLED, DefaultRoles.ADMIN);

        assertDenied(() -> this.policy.requireActive(pending), Reason.ACCOUNT_PENDING);
        assertDenied(() -> this.policy.requireActive(disabled), Reason.ACCOUNT_DISABLED);
        assertDenied(() -> this.policy.requireActive(UserId.newId()), Reason.UNKNOWN_USER);
    }

    private Permissions permissionsOf(final RoleKey key) {
        return DefaultRoles.all().stream()
                .filter(r -> r.key().equals(key))
                .findFirst()
                .orElseThrow()
                .permissions();
    }

    private UserId user(final UserStatus status, final RoleKey... roles) {
        return this.users.save(this.newUser(status).withManualRoles(Set.of(roles))).id();
    }

    private User newUser(final UserStatus status) {
        return User.register(
                new ExternalIdentity("test", UserId.newId().toString()),
                null,
                null,
                status,
                Set.of(),
                NOW);
    }
}
