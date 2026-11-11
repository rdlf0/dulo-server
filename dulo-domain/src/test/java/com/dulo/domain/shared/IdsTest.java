package com.dulo.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

class IdsTest {

    @Test
    void generatesVersion7Uuids() {
        final UUID id = Ids.newId();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void embedsTimestampInMostSignificantBits() {
        final Instant instant = Instant.parse("2026-11-10T12:00:00Z");
        final UUID id = Ids.newId(Clock.fixed(instant, ZoneOffset.UTC));

        assertThat(id.getMostSignificantBits() >>> 16).isEqualTo(instant.toEpochMilli());
    }

    @Test
    void laterIdsSortAfterEarlierOnes() {
        final UUID earlier = Ids.newId(Clock.fixed(Instant.ofEpochMilli(1_000), ZoneOffset.UTC));
        final UUID later = Ids.newId(Clock.fixed(Instant.ofEpochMilli(2_000), ZoneOffset.UTC));

        assertThat(earlier.toString()).isLessThan(later.toString());
    }
}
