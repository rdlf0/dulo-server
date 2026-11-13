package com.dulo.domain.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.dulo.domain.feature.Feature;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class DefaultRolesTest {

    private static final Map<RoleKey, Role> ROLES =
            DefaultRoles.all().stream().collect(Collectors.toMap(Role::key, Function.identity()));

    private static Permissions permissions(final RoleKey key) {
        return ROLES.get(key).permissions();
    }

    @Test
    void areBuiltInAndNew() {
        assertThat(DefaultRoles.all())
                .extracting(Role::key)
                .containsExactly(
                        DefaultRoles.ADMIN,
                        DefaultRoles.EDITOR,
                        DefaultRoles.CONTRIBUTOR,
                        DefaultRoles.VIEWER);
        assertThat(DefaultRoles.all()).allMatch(Role::builtIn).allMatch(r -> r.version() == 0);
    }

    @Test
    void onlyAdminAdministers() {
        assertThat(ROLES.get(DefaultRoles.ADMIN).permissions()).isEqualTo(Permissions.all());
        assertThat(DefaultRoles.all())
                .filteredOn(r -> !r.key().equals(DefaultRoles.ADMIN))
                .noneMatch(r -> r.permissions().allows(Feature.ADMINISTRATION, Access.VIEW));
    }

    @Test
    void editorDeletesButContributorDoesNot() {
        assertThat(permissions(DefaultRoles.EDITOR).allows(Feature.PEOPLE, Access.DELETE)).isTrue();
        assertThat(permissions(DefaultRoles.CONTRIBUTOR).allows(Feature.PEOPLE, Access.EDIT))
                .isTrue();
        assertThat(permissions(DefaultRoles.CONTRIBUTOR).allows(Feature.PEOPLE, Access.DELETE))
                .isFalse();
    }

    @Test
    void viewerOnlyViews() {
        final Permissions viewer = permissions(DefaultRoles.VIEWER);

        assertThat(viewer.allows(Feature.PLACES, Access.VIEW)).isTrue();
        assertThat(viewer.allows(Feature.MAP_VIEW, Access.VIEW)).isTrue();
        assertThat(viewer.allows(Feature.PLACES, Access.CREATE)).isFalse();
        assertThat(viewer.allows(Feature.AUDIT_LOG, Access.VIEW)).isFalse();
    }

    @Test
    void viewsAreReadOnly() {
        assertThat(permissions(DefaultRoles.EDITOR).accessTo(Feature.TIMELINE))
                .containsExactly(Access.VIEW);
        assertThat(permissions(DefaultRoles.EDITOR).accessTo(Feature.AUDIT_LOG))
                .containsExactly(Access.VIEW);
    }
}
