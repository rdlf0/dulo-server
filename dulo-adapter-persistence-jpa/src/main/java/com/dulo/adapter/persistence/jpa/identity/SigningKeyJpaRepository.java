package com.dulo.adapter.persistence.jpa.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SigningKeyJpaRepository extends JpaRepository<SigningKeyJpaEntity, String> {

    Optional<SigningKeyJpaEntity> findFirstByOrderByCreatedAtDesc();
}
