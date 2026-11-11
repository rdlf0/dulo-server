package com.dulo.domain.feature;

/**
 * The effective state of a feature.
 *
 * @param locked true when the deployment configuration forces the state, or the feature cannot be
 *     toggled at all
 */
public record FeatureState(Feature feature, boolean enabled, boolean locked) {}
