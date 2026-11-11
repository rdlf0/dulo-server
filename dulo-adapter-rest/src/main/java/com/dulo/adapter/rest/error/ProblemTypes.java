package com.dulo.adapter.rest.error;

import java.net.URI;

/** Problem type URIs (RFC 9457) returned by the API. Clients can switch on these values. */
public final class ProblemTypes {

    public static final URI TREE_NOT_INITIALIZED =
            URI.create("urn:dulo:problem:tree-not-initialized");
    public static final URI CONCURRENT_MODIFICATION =
            URI.create("urn:dulo:problem:concurrent-modification");

    private ProblemTypes() {}
}
