package com.dulo.core.identity.service;

import com.dulo.core.identity.port.in.SigningKeyUseCase;
import com.dulo.core.identity.port.out.SigningKeyRepository;
import com.dulo.core.shared.port.out.UnitOfWork;
import com.dulo.domain.identity.SigningKey;

import java.util.function.Supplier;

public class SigningKeyService implements SigningKeyUseCase {

    private final SigningKeyRepository keys;
    private final UnitOfWork unitOfWork;

    public SigningKeyService(final SigningKeyRepository keys, final UnitOfWork unitOfWork) {
        this.keys = keys;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public SigningKey currentOrCreate(final Supplier<SigningKey> generator) {
        return this.unitOfWork.write(
                () -> this.keys.findCurrent().orElseGet(() -> this.keys.save(generator.get())));
    }
}
