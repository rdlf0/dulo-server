package com.dulo.adapter.rest.error;

/** The initial setup has not created the family tree yet. */
public class TreeNotInitializedException extends RuntimeException {

    public TreeNotInitializedException() {
        super("The family tree has not been created yet. Complete the initial setup first.");
    }
}
