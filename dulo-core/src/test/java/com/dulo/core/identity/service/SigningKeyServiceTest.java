package com.dulo.core.identity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dulo.core.identity.port.out.SigningKeyRepository;
import com.dulo.core.testing.DirectUnitOfWork;
import com.dulo.domain.identity.SigningKey;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

class SigningKeyServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

    @Test
    void createsTheKeyOnceAndReusesIt() {
        final List<SigningKey> stored = new ArrayList<>();
        final SigningKeyRepository repository =
                new SigningKeyRepository() {
                    @Override
                    public Optional<SigningKey> findCurrent() {
                        return stored.stream().reduce((_, newer) -> newer);
                    }

                    @Override
                    public SigningKey save(final SigningKey key) {
                        stored.add(key);
                        return key;
                    }
                };
        final SigningKeyService service = new SigningKeyService(repository, new DirectUnitOfWork());
        final AtomicInteger generated = new AtomicInteger();

        final SigningKey first =
                service.currentOrCreate(
                        () -> new SigningKey("key-" + generated.incrementAndGet(), "{}", NOW));
        final SigningKey second =
                service.currentOrCreate(
                        () -> new SigningKey("key-" + generated.incrementAndGet(), "{}", NOW));

        assertThat(first.id()).isEqualTo("key-1");
        assertThat(second).isEqualTo(first);
        assertThat(generated).hasValue(1);
        assertThat(stored).hasSize(1);
    }
}
