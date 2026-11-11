package com.dulo.core.tree.port.out;

import com.dulo.domain.feature.FeatureOverrides;

/** Feature states forced by the deployment configuration (YAML or environment variables). */
public interface FeatureOverridesPort {

    FeatureOverrides overrides();
}
