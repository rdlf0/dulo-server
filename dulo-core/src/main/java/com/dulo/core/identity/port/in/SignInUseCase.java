package com.dulo.core.identity.port.in;

import com.dulo.domain.user.UserId;

/** Turns a sign-in at an identity provider into a Dulo user. */
public interface SignInUseCase {

    /**
     * Finds or creates the user and updates their profile and the roles their claims map to.
     *
     * <p>New users are active when the registration mode is {@code auto} or a role mapping matches
     * their claims, and pending otherwise. A pending user becomes active as soon as a role mapping
     * matches. Users whose claims match no mapping get the provider's default role.
     *
     * @return the Dulo user id, the subject of the tokens Dulo issues
     * @throws IllegalArgumentException if the provider is not configured
     */
    UserId signIn(ExternalSignIn signIn);
}
