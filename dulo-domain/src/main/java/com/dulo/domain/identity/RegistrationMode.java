package com.dulo.domain.identity;

/** What happens when an unknown user signs in for the first time. */
public enum RegistrationMode {
    /** New users are active right away. */
    AUTO,
    /** New users wait for an administrator's approval, unless a role mapping matches. */
    APPROVAL,
    ;
}
