package com.dulo.domain.identity;

import com.dulo.domain.access.RoleKey;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * How an identity provider's role claim translates to Dulo roles.
 *
 * @param byClaimValue Dulo roles per value of the provider's roles claim, e.g. {@code family-admins
 *     -> [admin]}
 * @param defaultRole the role of users whose claims match no mapping
 */
public record RoleMapping(Map<String, Set<RoleKey>> byClaimValue, @Nullable RoleKey defaultRole) {

    public RoleMapping {
        final Map<String, Set<RoleKey>> copy = new HashMap<>();
        byClaimValue.forEach((value, roles) -> copy.put(value, Set.copyOf(roles)));
        byClaimValue = Map.copyOf(copy);
    }

    public static RoleMapping none() {
        return new RoleMapping(Map.of(), null);
    }

    /** The roles the given claim values map to, empty when no mapping matches. */
    public Set<RoleKey> mappedRoles(final Set<String> claimValues) {
        final Set<RoleKey> roles = new HashSet<>();
        claimValues.forEach(value -> roles.addAll(byClaimValue.getOrDefault(value, Set.of())));
        return Set.copyOf(roles);
    }

    public Optional<RoleKey> defaultRoleIfAny() {
        return Optional.ofNullable(defaultRole);
    }
}
