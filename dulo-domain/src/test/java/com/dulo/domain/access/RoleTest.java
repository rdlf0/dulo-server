package com.dulo.domain.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RoleTest {

    private static final RoleKey KEY = new RoleKey("x");

    @ParameterizedTest
    @ValueSource(strings = {"admin", "family-editor", "v2"})
    void acceptsSlugKeys(final String key) {
        assertThat(new RoleKey(key)).hasToString(key);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Admin", "family_editor", "-admin", "admin-", "a--b", "a b"})
    void rejectsOtherKeys(final String key) {
        assertThatThrownBy(() -> new RoleKey(key)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTooLongKeys() {
        assertThat(new RoleKey("a".repeat(RoleKey.MAX_LENGTH)).value()).hasSize(50);
        assertThatThrownBy(() -> new RoleKey("a".repeat(RoleKey.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankNames(final String name) {
        assertThatThrownBy(() -> new Role(KEY, name, false, Permissions.none(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTooLongNames() {
        assertThatThrownBy(() ->
                        new Role(
                                KEY,
                                "x".repeat(Role.NAME_MAX_LENGTH + 1),
                                false,
                                Permissions.none(),
                                0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeVersions() {
        assertThatThrownBy(() -> new Role(KEY, "X", false, Permissions.none(), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stripsNames() {
        assertThat(new Role(KEY, "  Family  ", false, Permissions.none(), 0).name())
                .isEqualTo("Family");
    }
}
