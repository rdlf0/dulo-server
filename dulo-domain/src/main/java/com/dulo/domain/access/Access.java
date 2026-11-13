package com.dulo.domain.access;

import java.util.Locale;

/** What a user may do within a {@link com.dulo.domain.feature.Feature}. */
public enum Access {
    VIEW,
    CREATE,
    EDIT,
    DELETE,
    ;

    /** Stable external name, e.g. {@code view}. */
    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
