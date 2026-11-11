package com.dulo.core.tree.port.out;

import com.dulo.domain.tree.TreeSettings;

import java.util.Optional;

/** Stores the single family tree of this instance. */
public interface TreeSettingsRepository {

    /** The tree, or empty when the initial setup has not created it yet. */
    Optional<TreeSettings> find();

    /**
     * Inserts a new tree or updates the existing one.
     *
     * @return the stored tree with its new version
     * @throws com.dulo.domain.shared.ConcurrentModificationException if {@code tree.version()} is
     *     stale
     */
    TreeSettings save(TreeSettings tree);
}
