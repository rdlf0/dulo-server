package com.dulo.core.testing;

import com.dulo.core.tree.port.out.TreeSettingsRepository;
import com.dulo.domain.tree.TreeSettings;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/** Holds at most one tree. Saving does not check versions. */
public final class InMemoryTrees implements TreeSettingsRepository {

    private @Nullable TreeSettings tree;

    public InMemoryTrees(final @Nullable TreeSettings tree) {
        this.tree = tree;
    }

    @Override
    public Optional<TreeSettings> find() {
        return Optional.ofNullable(this.tree);
    }

    @Override
    public TreeSettings save(final TreeSettings tree) {
        this.tree = tree;
        return tree;
    }

    /** Removes the tree, as before the initial setup. */
    public void clear() {
        this.tree = null;
    }
}
