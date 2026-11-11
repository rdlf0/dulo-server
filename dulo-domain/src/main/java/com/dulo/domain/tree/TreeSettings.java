package com.dulo.domain.tree;

import com.dulo.domain.feature.Feature;
import com.dulo.domain.feature.FeatureOverrides;
import com.dulo.domain.feature.FeatureState;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The single family tree of a Dulo instance and the features the administrator enabled for it.
 *
 * @param enabledFeatures features the administrator switched on; configuration overrides may still
 *     force a different effective state, see {@link #featureStates(FeatureOverrides)}
 * @param version optimistic-locking version, 0 for a tree that has not been stored yet
 */
public record TreeSettings(
        TreeId id,
        String name,
        @Nullable String description,
        Set<Feature> enabledFeatures,
        long version) {

    public static final int NAME_MAX_LENGTH = 200;
    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    public TreeSettings {
        Objects.requireNonNull(id, "id");
        name = Objects.requireNonNull(name, "name").strip();
        if (name.isEmpty() || name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Tree name must be 1-%d characters".formatted(NAME_MAX_LENGTH));
        }
        description = description == null || description.isBlank() ? null : description.strip();
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Tree description must be at most %d characters"
                            .formatted(DESCRIPTION_MAX_LENGTH));
        }
        final EnumSet<Feature> features = EnumSet.noneOf(Feature.class);
        features.addAll(enabledFeatures);
        // Features that cannot be toggled are always on.
        Arrays.stream(Feature.values()).filter(f -> !f.toggleable()).forEach(features::add);
        enabledFeatures = Set.copyOf(features);
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public static TreeSettings create(
            final String name,
            final @Nullable String description,
            final Set<Feature> enabledFeatures) {
        return new TreeSettings(TreeId.newId(), name, description, enabledFeatures, 0);
    }

    /** Effective state of every feature, with configuration overrides applied. */
    public List<FeatureState> featureStates(final FeatureOverrides overrides) {
        return Arrays.stream(Feature.values()).map(f -> this.featureState(f, overrides)).toList();
    }

    public boolean isEnabled(final Feature feature, final FeatureOverrides overrides) {
        return this.featureState(feature, overrides).enabled();
    }

    private FeatureState featureState(final Feature feature, final FeatureOverrides overrides) {
        if (!feature.toggleable()) {
            return new FeatureState(feature, true, true);
        }
        return overrides
                .forcedState(feature)
                .map(forced -> new FeatureState(feature, forced, true))
                .orElseGet(
                        () -> new FeatureState(feature, enabledFeatures.contains(feature), false));
    }
}
