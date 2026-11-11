package com.dulo.domain.feature;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Feature states forced by the deployment configuration. A forced feature cannot be changed by an
 * administrator; features without an override follow the stored tree settings.
 */
public record FeatureOverrides(Map<Feature, Boolean> forced) {

    public FeatureOverrides {
        forced = forced.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(forced));
        forced.keySet().stream()
                .filter(f -> !f.toggleable())
                .findAny()
                .ifPresent(f -> {
                    throw new IllegalArgumentException(
                            "Feature '%s' is always enabled and cannot be overridden"
                                    .formatted(f.key()));
                });
    }

    public static FeatureOverrides none() {
        return new FeatureOverrides(Map.of());
    }

    public Optional<Boolean> forcedState(final Feature feature) {
        return Optional.ofNullable(forced.get(feature));
    }
}
