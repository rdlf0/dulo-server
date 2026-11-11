package com.dulo.adapter.rest.error;

import com.dulo.domain.shared.ConcurrentModificationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/** Maps exceptions to RFC 9457 problem details. */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static ProblemDetail problem(
            final HttpStatus status, final URI type, final String title, final Exception e) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setType(type);
        problem.setTitle(title);
        return problem;
    }

    @ExceptionHandler
    ProblemDetail treeNotInitialized(final TreeNotInitializedException e) {
        return problem(
                HttpStatus.NOT_FOUND, ProblemTypes.TREE_NOT_INITIALIZED, "Tree not initialized", e);
    }

    @ExceptionHandler
    ProblemDetail concurrentModification(final ConcurrentModificationException e) {
        return problem(
                HttpStatus.CONFLICT,
                ProblemTypes.CONCURRENT_MODIFICATION,
                "Concurrent modification",
                e);
    }
}
