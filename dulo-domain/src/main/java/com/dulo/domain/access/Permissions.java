package com.dulo.domain.access;

import com.dulo.domain.feature.Feature;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The access granted per feature. Features without an entry grant nothing.
 *
 * @param grants the access granted per feature; empty sets are dropped
 */
public record Permissions(Map<Feature, Set<Access>> grants) {

    public Permissions {
        final Map<Feature, Set<Access>> copy = new EnumMap<>(Feature.class);
        grants.forEach((feature, access) -> {
            if (!access.isEmpty()) {
                copy.put(feature, Set.copyOf(EnumSet.copyOf(access)));
            }
        });
        grants = Map.copyOf(copy);
    }

    public static Permissions none() {
        return new Permissions(Map.of());
    }

    /** Every access on every feature. */
    public static Permissions all() {
        final Map<Feature, Set<Access>> grants = new EnumMap<>(Feature.class);
        Arrays.stream(Feature.values()).forEach(f -> grants.put(f, EnumSet.allOf(Access.class)));
        return new Permissions(grants);
    }

    public boolean allows(final Feature feature, final Access access) {
        return grants.getOrDefault(feature, Set.of()).contains(access);
    }

    /** The access granted on a feature, empty when none. */
    public Set<Access> accessTo(final Feature feature) {
        return grants.getOrDefault(feature, Set.of());
    }

    /** Everything either this or the other permissions grant. */
    public Permissions union(final Permissions other) {
        final Map<Feature, Set<Access>> merged = new EnumMap<>(Feature.class);
        for (final Feature feature : Feature.values()) {
            final Set<Access> access = EnumSet.noneOf(Access.class);
            access.addAll(this.accessTo(feature));
            access.addAll(other.accessTo(feature));
            merged.put(feature, access);
        }
        return new Permissions(merged);
    }
}
