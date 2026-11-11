package com.dulo.domain.shared;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

/**
 * Generates time-ordered UUIDv7 identifiers (RFC 9562). Sorting by ID roughly follows creation
 * order, which keeps database indexes compact.
 */
public final class Ids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {}

    public static UUID newId() {
        return newId(Clock.systemUTC());
    }

    static UUID newId(final Clock clock) {
        final long millis = clock.millis();
        final byte[] random = new byte[10];
        RANDOM.nextBytes(random);

        // 48-bit timestamp, 4-bit version (7), 12 random bits.
        long msb = (millis & 0xFFFF_FFFF_FFFFL) << 16;
        msb |= 0x7000L;
        msb |= ((random[0] & 0x0FL) << 8) | (random[1] & 0xFFL);

        // 2-bit variant (10), 62 random bits.
        long lsb = 0;
        for (int i = 2; i < 10; i++) {
            lsb = (lsb << 8) | (random[i] & 0xFFL);
        }
        lsb = (lsb & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;

        return new UUID(msb, lsb);
    }
}
