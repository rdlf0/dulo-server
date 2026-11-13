package com.dulo.adapter.persistence.jpa.role;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, String> {

    List<RoleJpaEntity> findAllByOrderByRoleKey();

    List<RoleJpaEntity> findAllByRoleKeyInOrderByRoleKey(Collection<String> roleKeys);
}
