package com.dulo.core.access.port.out;

import com.dulo.domain.user.ExternalIdentity;
import com.dulo.domain.user.User;
import com.dulo.domain.user.UserId;

import java.util.Optional;

/** Stores the users of this instance. */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByExternalIdentity(ExternalIdentity identity);

    /**
     * Inserts a new user or updates an existing one.
     *
     * @return the stored user with its new version
     * @throws com.dulo.domain.shared.ConcurrentModificationException if {@code user.version()} is
     *     stale
     */
    User save(User user);
}
