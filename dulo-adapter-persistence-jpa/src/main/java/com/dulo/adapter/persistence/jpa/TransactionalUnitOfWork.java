package com.dulo.adapter.persistence.jpa;

import com.dulo.core.shared.port.out.UnitOfWork;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

class TransactionalUnitOfWork implements UnitOfWork {

    private final TransactionTemplate readTx;
    private final TransactionTemplate writeTx;

    TransactionalUnitOfWork(final PlatformTransactionManager transactionManager) {
        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
        this.writeTx = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T read(final Supplier<T> work) {
        return readTx.execute(_ -> work.get());
    }

    @Override
    public <T> T write(final Supplier<T> work) {
        return writeTx.execute(_ -> work.get());
    }
}
