package com.dulo.adapter.rest.tree;

import com.dulo.adapter.rest.error.TreeNotInitializedException;
import com.dulo.core.tree.port.in.GetTreeUseCase;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO(M2): require an authenticated user once the authorization server is in place.
@RestController
@RequestMapping("/api/v1/tree")
class TreeController {

    private final GetTreeUseCase getTree;

    TreeController(final GetTreeUseCase getTree) {
        this.getTree = getTree;
    }

    @GetMapping
    ResponseEntity<TreeResponse> get() {
        final var view = getTree.getTree().orElseThrow(TreeNotInitializedException::new);
        return ResponseEntity.ok()
                .eTag(Long.toString(view.tree().version()))
                .body(TreeResponse.from(view));
    }
}
