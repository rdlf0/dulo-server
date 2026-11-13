package com.dulo.domain.access;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable identifier of a role, e.g. {@code editor}. Configuration refers to roles by this key, so
 * it never changes once the role exists.
 */
public record RoleKey(String value) {

    public static final int MAX_LENGTH = 50;

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    public RoleKey {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Role key '%s' must be 1-%d lowercase letters, digits and single hyphens"
                            .formatted(value, MAX_LENGTH));
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
