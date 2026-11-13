package com.dulo.domain.user;

public enum UserStatus {
    /** May use Dulo within the permissions of their roles. */
    ACTIVE,
    /** Signed in, waiting for an administrator's approval. */
    PENDING,
    /** Blocked by an administrator. */
    DISABLED,
    ;
}
