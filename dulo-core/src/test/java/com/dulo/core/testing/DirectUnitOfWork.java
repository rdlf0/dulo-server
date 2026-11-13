package com.dulo.core.testing;

import com.dulo.core.shared.port.out.UnitOfWork;

import java.util.function.Supplier;

/** Runs the work right away, without a transaction. */
public final class DirectUnitOfWork implements UnitOfWork {

    @Override
    public <T> T read(final Supplier<T> work) {
        return work.get();
    }

    @Override
    public <T> T write(final Supplier<T> work) {
        return work.get();
    }
}
