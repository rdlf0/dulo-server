package com.dulo.domain.feature;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** A functional area of Dulo that can be switched on or off and guarded by permissions. */
public enum Feature {
    PEOPLE(true),
    FAMILIES(true),
    EVENTS(true),
    PLACES(true),
    TREE_VIEW(true),
    MAP_VIEW(true),
    TIMELINE(true),
    AUDIT_LOG(true),
    /** Always on, otherwise nobody could switch features back on. */
    ADMINISTRATION(false),
    ;

    private final boolean toggleable;

    Feature(final boolean toggleable) {
        this.toggleable = toggleable;
    }

    /**
     * Resolves a feature from a key, ignoring case and separators, so {@code map-view}, {@code
     * map_view}, {@code map.view} and {@code MAP_VIEW} all match.
     */
    public static Optional<Feature> fromKey(final @Nullable String key) {
        if (key == null) {
            return Optional.empty();
        }
        final String normalized = normalize(key);
        return Arrays.stream(values())
                .filter(f -> normalize(f.name()).equals(normalized))
                .findFirst();
    }

    private static String normalize(final String key) {
        return key.replaceAll("[-_.\\s]", "").toLowerCase(Locale.ROOT);
    }

    public boolean toggleable() {
        return toggleable;
    }

    /** Stable external name, e.g. {@code map-view}. */
    public String key() {
        return this.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
