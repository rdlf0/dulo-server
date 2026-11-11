package com.dulo.domain.tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dulo.domain.feature.Feature;
import com.dulo.domain.feature.FeatureOverrides;
import com.dulo.domain.feature.FeatureState;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class TreeSettingsTest {

    @Test
    void administrationIsAlwaysEnabled() {
        final TreeSettings tree = TreeSettings.create("Smiths", null, Set.of());

        assertThat(tree.enabledFeatures()).containsExactly(Feature.ADMINISTRATION);
    }

    @Test
    void stripsNameAndDropsBlankDescription() {
        final TreeSettings tree = TreeSettings.create("  Smiths  ", "   ", Set.of());

        assertThat(tree.name()).isEqualTo("Smiths");
        assertThat(tree.description()).isNull();
    }

    @Test
    void rejectsBlankOrTooLongName() {
        assertThatThrownBy(() -> TreeSettings.create(" ", null, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TreeSettings.create("x".repeat(201), null, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void storedStateAppliesWhenNotOverridden() {
        final TreeSettings tree = TreeSettings.create("Smiths", null, Set.of(Feature.PEOPLE));

        assertThat(tree.featureStates(FeatureOverrides.none()))
                .contains(
                        new FeatureState(Feature.PEOPLE, true, false),
                        new FeatureState(Feature.MAP_VIEW, false, false),
                        new FeatureState(Feature.ADMINISTRATION, true, true));
    }

    @Test
    void configurationOverrideWinsAndLocksFeature() {
        final TreeSettings tree = TreeSettings.create("Smiths", null, Set.of(Feature.PEOPLE));
        final FeatureOverrides overrides =
                new FeatureOverrides(Map.of(Feature.PEOPLE, false, Feature.MAP_VIEW, true));

        assertThat(tree.featureStates(overrides))
                .contains(
                        new FeatureState(Feature.PEOPLE, false, true),
                        new FeatureState(Feature.MAP_VIEW, true, true));
        assertThat(tree.isEnabled(Feature.PEOPLE, overrides)).isFalse();
    }
}
