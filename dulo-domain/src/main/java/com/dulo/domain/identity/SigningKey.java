package com.dulo.domain.identity;

import java.time.Instant;
import java.util.Objects;

/**
 * A key Dulo signs its access tokens with. The domain does not interpret the key material.
 *
 * @param id the key id, published in the token header ({@code kid})
 * @param material the serialized key pair, including the private key
 */
public record SigningKey(String id, String material, Instant createdAt) {

    public static final int ID_MAX_LENGTH = 64;

    public SigningKey {
        Objects.requireNonNull(id, "id");
        if (id.isEmpty() || id.length() > ID_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Signing key id must be 1-%d characters".formatted(ID_MAX_LENGTH));
        }
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Leaves out the key material, which holds the private key. */
    @Override
    public String toString() {
        return "SigningKey[id=%s, createdAt=%s]".formatted(id, createdAt);
    }
}
