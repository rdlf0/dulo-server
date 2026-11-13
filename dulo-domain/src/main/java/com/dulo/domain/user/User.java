package com.dulo.domain.user;

import com.dulo.domain.access.RoleKey;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A person who signs in to Dulo through an identity provider.
 *
 * @param email the email address the provider reported at the last sign-in
 * @param displayName the name the provider reported at the last sign-in
 * @param root true for the user who completed the initial setup; bypasses all permission checks
 * @param claimRoles roles derived from the provider's claims, recomputed at every sign-in
 * @param manualRoles roles an administrator assigned
 * @param version optimistic-locking version, 0 for a user that has not been stored yet
 */
public record User(
        UserId id,
        ExternalIdentity externalIdentity,
        @Nullable String email,
        @Nullable String displayName,
        UserStatus status,
        boolean root,
        Set<RoleKey> claimRoles,
        Set<RoleKey> manualRoles,
        Instant lastSignIn,
        long version) {

    public static final int EMAIL_MAX_LENGTH = 320;
    public static final int DISPLAY_NAME_MAX_LENGTH = 200;

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        email = clean(email, EMAIL_MAX_LENGTH);
        displayName = clean(displayName, DISPLAY_NAME_MAX_LENGTH);
        Objects.requireNonNull(status, "status");
        claimRoles = Set.copyOf(claimRoles);
        manualRoles = Set.copyOf(manualRoles);
        Objects.requireNonNull(lastSignIn, "lastSignIn");
        if (root && status != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("The root user must be active");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    /** A user signing in for the first time. */
    public static User register(
            final ExternalIdentity externalIdentity,
            final @Nullable String email,
            final @Nullable String displayName,
            final UserStatus status,
            final Set<RoleKey> claimRoles,
            final Instant signedInAt) {
        return new User(
                UserId.newId(),
                externalIdentity,
                email,
                displayName,
                status,
                false,
                claimRoles,
                Set.of(),
                signedInAt,
                0);
    }

    /**
     * Blank values become null; longer values are cut, since they come from the provider and must
     * not block the sign-in.
     */
    private static @Nullable String clean(final @Nullable String value, final int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        final String stripped = value.strip();
        return stripped.length() > maxLength ? stripped.substring(0, maxLength) : stripped;
    }

    /** All roles of the user: from claims and assigned by hand. */
    public Set<RoleKey> roles() {
        final Set<RoleKey> roles = new HashSet<>(claimRoles);
        roles.addAll(manualRoles);
        return Set.copyOf(roles);
    }

    /** The user after another sign-in, with the provider's current profile and claim roles. */
    public User signedIn(
            final @Nullable String email,
            final @Nullable String displayName,
            final Set<RoleKey> claimRoles,
            final Instant signedInAt) {
        return new User(
                id,
                externalIdentity,
                email,
                displayName,
                status,
                root,
                claimRoles,
                manualRoles,
                signedInAt,
                version);
    }

    public User withStatus(final UserStatus status) {
        return new User(
                id,
                externalIdentity,
                email,
                displayName,
                status,
                root,
                claimRoles,
                manualRoles,
                lastSignIn,
                version);
    }

    public User withManualRoles(final Set<RoleKey> manualRoles) {
        return new User(
                id,
                externalIdentity,
                email,
                displayName,
                status,
                root,
                claimRoles,
                manualRoles,
                lastSignIn,
                version);
    }

    /** The user as root: active and exempt from permission checks. */
    public User asRoot() {
        return new User(
                id,
                externalIdentity,
                email,
                displayName,
                UserStatus.ACTIVE,
                true,
                claimRoles,
                manualRoles,
                lastSignIn,
                version);
    }
}
