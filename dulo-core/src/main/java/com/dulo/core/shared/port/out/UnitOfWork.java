package com.dulo.core.shared.port.out;

import java.util.function.Supplier;

/** Runs application logic inside a transaction provided by the persistence adapter. */
public interface UnitOfWork {

    <T> T read(Supplier<T> work);

    <T> T write(Supplier<T> work);
}
