package com.dulo.domain.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

class FeatureTest {

    @ParameterizedTest
    @ValueSource(strings = {"map-view", "map_view", "map.view", "MAP_VIEW", "mapView"})
    void resolvesKeysIgnoringCaseAndSeparators(final String key) {
        assertThat(Feature.fromKey(key)).contains(Feature.MAP_VIEW);
    }

    @Test
    void unknownKeyResolvesToEmpty() {
        assertThat(Feature.fromKey("chat")).isEmpty();
        assertThat(Feature.fromKey(null)).isEmpty();
    }

    @Test
    void keyIsKebabCase() {
        assertThat(Feature.AUDIT_LOG.key()).isEqualTo("audit-log");
    }

    @Test
    void administrationCannotBeOverridden() {
        assertThatThrownBy(() -> new FeatureOverrides(Map.of(Feature.ADMINISTRATION, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("administration");
    }
}
