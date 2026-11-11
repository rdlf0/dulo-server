package com.dulo.domain.shared;

/** Thrown when an update is based on a stale version of an aggregate. */
public class ConcurrentModificationException extends RuntimeException {

    public ConcurrentModificationException(
            final String aggregate, final Object id, final long expected, final long actual) {
        super(
                "%s %s was modified concurrently (expected version %d, found %d)"
                        .formatted(aggregate, id, expected, actual));
    }
}
