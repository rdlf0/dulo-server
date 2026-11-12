package com.dulo.adapter.rest.tree;

import com.dulo.adapter.rest.error.TreeNotInitializedException;
import com.dulo.core.tree.port.in.GetTreeUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO(M2): require an authenticated user once the authorization server is in place.
@Tag(name = "Tree", description = "The family tree of this server")
@RestController
@RequestMapping("/api/v1/tree")
class TreeController {

    private final GetTreeUseCase getTree;

    TreeController(final GetTreeUseCase getTree) {
        this.getTree = getTree;
    }

    @Operation(
            summary = "Get the family tree",
            description = "Returns the tree's name, description and the state of each feature.")
    @ApiResponse(
            responseCode = "200",
            description = "The tree",
            headers =
                    @Header(
                            name = "ETag",
                            description = "Version of the tree",
                            schema = @Schema(type = "string")))
    @ApiResponse(
            responseCode = "404",
            description =
                    "The initial setup is not done yet. The problem type is"
                            + " `urn:dulo:problem:tree-not-initialized`.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TreeResponse> getTree() {
        final var view = this.getTree.getTree().orElseThrow(TreeNotInitializedException::new);
        return ResponseEntity.ok()
                .eTag(Long.toString(view.tree().version()))
                .body(TreeResponse.from(view));
    }
}
