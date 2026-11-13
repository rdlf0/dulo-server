package com.dulo.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.dulo.domain.access.RoleKey;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class RoleMappingTest {

    private static final RoleKey ADMIN = new RoleKey("admin");
    private static final RoleKey EDITOR = new RoleKey("editor");
    private static final RoleKey VIEWER = new RoleKey("viewer");

    private static final RoleMapping MAPPING =
            new RoleMapping(
                    Map.of("family-admins", Set.of(ADMIN, EDITOR), "family", Set.of(EDITOR)),
                    VIEWER);

    @Test
    void mapsEveryMatchingClaimValue() {
        assertThat(MAPPING.mappedRoles(Set.of("family-admins", "family", "other")))
                .containsExactlyInAnyOrder(ADMIN, EDITOR);
    }

    @Test
    void mapsNothingWithoutAMatch() {
        assertThat(MAPPING.mappedRoles(Set.of("other"))).isEmpty();
        assertThat(MAPPING.defaultRoleIfAny()).contains(VIEWER);
    }

    @Test
    void noneMapsNothing() {
        assertThat(RoleMapping.none().mappedRoles(Set.of("family"))).isEmpty();
        assertThat(RoleMapping.none().defaultRoleIfAny()).isEmpty();
    }
}
