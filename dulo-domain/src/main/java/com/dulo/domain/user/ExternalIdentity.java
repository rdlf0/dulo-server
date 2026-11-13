package com.dulo.domain.user;

import java.util.Objects;

/**
 * Who a user is at an identity provider.
 *
 * @param providerId the provider's id in the Dulo configuration, e.g. {@code google}
 * @param subject the provider's stable identifier of the user (the OIDC {@code sub} claim)
 */
public record ExternalIdentity(String providerId, String subject) {

    public static final int PROVIDER_ID_MAX_LENGTH = 50;
    public static final int SUBJECT_MAX_LENGTH = 255;

    public ExternalIdentity {
        requireLength(providerId, "providerId", PROVIDER_ID_MAX_LENGTH);
        requireLength(subject, "subject", SUBJECT_MAX_LENGTH);
    }

    private static void requireLength(final String value, final String name, final int max) {
        Objects.requireNonNull(value, name);
        if (value.isEmpty() || value.length() > max) {
            throw new IllegalArgumentException("%s must be 1-%d characters".formatted(name, max));
        }
    }
}
