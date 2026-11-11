package com.dulo.core.tree.port.in;

import com.dulo.domain.feature.FeatureState;
import com.dulo.domain.tree.TreeSettings;

import java.util.List;

/** The tree together with the effective state of every feature. */
public record TreeView(TreeSettings tree, List<FeatureState> features) {

    public TreeView {
        features = List.copyOf(features);
    }
}
