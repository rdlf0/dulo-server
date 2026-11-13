package com.dulo.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.time.Instant;

class SigningKeyTest {

    private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

    @Test
    void toStringHidesTheKeyMaterial() {
        final SigningKey key = new SigningKey("key-1", "{\"d\":\"secret\"}", NOW);

        assertThat(key.toString()).contains("key-1").doesNotContain("secret");
    }

    @Test
    void rejectsBlankOrTooLongIds() {
        assertThatThrownBy(() -> new SigningKey("", "{}", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SigningKey("x".repeat(65), "{}", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
