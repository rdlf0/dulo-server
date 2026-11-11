package com.dulo.core.tree.service;

import com.dulo.core.shared.port.out.UnitOfWork;
import com.dulo.core.tree.port.in.GetTreeUseCase;
import com.dulo.core.tree.port.in.TreeView;
import com.dulo.core.tree.port.out.FeatureOverridesPort;
import com.dulo.core.tree.port.out.TreeSettingsRepository;

import java.util.Optional;

// TODO(M2): check the caller's permissions through AccessPolicy.
public class TreeService implements GetTreeUseCase {

    private final TreeSettingsRepository trees;
    private final FeatureOverridesPort featureOverrides;
    private final UnitOfWork unitOfWork;

    public TreeService(
            final TreeSettingsRepository trees,
            final FeatureOverridesPort featureOverrides,
            final UnitOfWork unitOfWork) {
        this.trees = trees;
        this.featureOverrides = featureOverrides;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Optional<TreeView> getTree() {
        final var overrides = featureOverrides.overrides();
        return unitOfWork
                .read(trees::find)
                .map(tree -> new TreeView(tree, tree.featureStates(overrides)));
    }
}
