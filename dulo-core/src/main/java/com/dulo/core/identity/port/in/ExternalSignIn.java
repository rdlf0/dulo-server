package com.dulo.core.identity.port.in;

import com.dulo.domain.user.ExternalIdentity;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

/**
 * A successful sign-in at an identity provider, with the claims Dulo reads.
 *
 * @param roleClaimValues values of the provider's roles claim, empty when it sends none
 */
public record ExternalSignIn(
        ExternalIdentity identity,
        @Nullable String email,
        @Nullable String displayName,
        Set<String> roleClaimValues) {

    public ExternalSignIn {
        Objects.requireNonNull(identity, "identity");
        roleClaimValues = Set.copyOf(roleClaimValues);
    }
}
