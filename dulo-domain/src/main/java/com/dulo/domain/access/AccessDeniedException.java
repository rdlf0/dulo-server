package com.dulo.domain.access;

/** The caller may not perform the requested operation. */
public class AccessDeniedException extends RuntimeException {

    private final Reason reason;

    public AccessDeniedException(final Reason reason, final String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    /** Why access was denied, so clients can tell the user what to do. */
    public enum Reason {
        /** None of the user's roles grants the access. */
        NOT_PERMITTED,
        /** The feature is switched off for everyone. */
        FEATURE_DISABLED,
        /** The user signed in but an administrator has not approved them yet. */
        ACCOUNT_PENDING,
        /** An administrator disabled the user. */
        ACCOUNT_DISABLED,
        /** The user does not exist, e.g. a token for a deleted user. */
        UNKNOWN_USER,
        ;
    }
}
