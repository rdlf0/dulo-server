package com.dulo.core.access.port.out;

import com.dulo.domain.access.Role;
import com.dulo.domain.access.RoleKey;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Stores the roles of this instance. */
public interface RoleRepository {

    /** All roles, sorted by key. */
    List<Role> findAll();

    /** The roles with the given keys. Keys without a stored role are skipped. */
    List<Role> findAllByKeys(Set<RoleKey> keys);

    Optional<Role> find(RoleKey key);

    /**
     * Inserts a new role or updates an existing one.
     *
     * @return the stored role with its new version
     * @throws com.dulo.domain.shared.ConcurrentModificationException if {@code role.version()} is
     *     stale
     */
    Role save(Role role);
}
