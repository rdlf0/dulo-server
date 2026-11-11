package com.dulo.core.tree.port.in;

import java.util.Optional;

public interface GetTreeUseCase {

    /** The tree, or empty when the initial setup has not created it yet. */
    Optional<TreeView> getTree();
}
