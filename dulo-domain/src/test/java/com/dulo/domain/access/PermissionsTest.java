package com.dulo.domain.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.dulo.domain.feature.Feature;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class PermissionsTest {

    @Test
    void noneAllowsNothing() {
        assertThat(Permissions.none().allows(Feature.PEOPLE, Access.VIEW)).isFalse();
        assertThat(Permissions.none().accessTo(Feature.PEOPLE)).isEmpty();
    }

    @Test
    void allAllowsEverything() {
        for (final Feature feature : Feature.values()) {
            for (final Access access : Access.values()) {
                assertThat(Permissions.all().allows(feature, access)).isTrue();
            }
        }
    }

    @Test
    void dropsEmptyGrants() {
        final Permissions permissions =
                new Permissions(
                        Map.of(Feature.PEOPLE, Set.of(), Feature.PLACES, Set.of(Access.VIEW)));

        assertThat(permissions.grants()).containsOnlyKeys(Feature.PLACES);
    }

    @Test
    void unionCombinesGrants() {
        final Permissions viewPeople = new Permissions(Map.of(Feature.PEOPLE, Set.of(Access.VIEW)));
        final Permissions editPeopleViewPlaces =
                new Permissions(
                        Map.of(
                                Feature.PEOPLE, Set.of(Access.EDIT),
                                Feature.PLACES, Set.of(Access.VIEW)));

        final Permissions union = viewPeople.union(editPeopleViewPlaces);

        assertThat(union.accessTo(Feature.PEOPLE))
                .containsExactlyInAnyOrder(Access.VIEW, Access.EDIT);
        assertThat(union.accessTo(Feature.PLACES)).containsExactly(Access.VIEW);
        assertThat(union.grants()).containsOnlyKeys(Feature.PEOPLE, Feature.PLACES);
    }

    @Test
    void accessKeyIsLowercase() {
        assertThat(Access.DELETE.key()).isEqualTo("delete");
    }
}
