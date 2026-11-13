package com.dulo.domain.user;

import com.dulo.domain.shared.Ids;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value");
    }

    public static UserId newId() {
        return new UserId(Ids.newId());
    }

    /**
     * Parses the string form, as used in token subjects.
     *
     * @throws IllegalArgumentException if the value is not a UUID
     */
    public static UserId parse(final String value) {
        return new UserId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
