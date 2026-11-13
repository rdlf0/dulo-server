package com.dulo.adapter.persistence.jpa.identity;

import com.dulo.core.identity.port.out.SigningKeyRepository;
import com.dulo.domain.identity.SigningKey;

import java.util.Optional;

public class SigningKeyPersistenceAdapter implements SigningKeyRepository {

    private final SigningKeyJpaRepository repository;

    SigningKeyPersistenceAdapter(final SigningKeyJpaRepository repository) {
        this.repository = repository;
    }

    private static SigningKey toDomain(final SigningKeyJpaEntity entity) {
        return new SigningKey(entity.getId(), entity.getMaterial(), entity.getCreatedAt());
    }

    @Override
    public Optional<SigningKey> findCurrent() {
        return this.repository
                .findFirstByOrderByCreatedAtDesc()
                .map(SigningKeyPersistenceAdapter::toDomain);
    }

    @Override
    public SigningKey save(final SigningKey key) {
        return toDomain(
                this.repository.saveAndFlush(
                        new SigningKeyJpaEntity(key.id(), key.material(), key.createdAt())));
    }
}
