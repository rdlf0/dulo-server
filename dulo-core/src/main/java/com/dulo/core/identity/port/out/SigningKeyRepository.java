package com.dulo.core.identity.port.out;

import com.dulo.domain.identity.SigningKey;

import java.util.Optional;

/** Stores the keys Dulo signs its tokens with. */
public interface SigningKeyRepository {

    /** The newest key, or empty before the first start. */
    Optional<SigningKey> findCurrent();

    SigningKey save(SigningKey key);
}
