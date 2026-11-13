package com.dulo.adapter.persistence.jpa.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByProviderIdAndExternalSubject(
            String providerId, String externalSubject);
}
