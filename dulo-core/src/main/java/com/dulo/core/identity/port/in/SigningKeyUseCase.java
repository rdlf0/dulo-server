package com.dulo.core.identity.port.in;

import com.dulo.domain.identity.SigningKey;

import java.util.function.Supplier;

/** The key Dulo signs its tokens with. It is kept in the database, so tokens survive restarts. */
public interface SigningKeyUseCase {

    /**
     * Returns the stored key, or stores and returns a new one on the first start.
     *
     * @param generator creates a new key; called only when none is stored
     */
    SigningKey currentOrCreate(Supplier<SigningKey> generator);
}
