package com.dulo.adapter.persistence.jpa.identity;

import com.dulo.core.identity.port.out.SigningKeyRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SigningKeyPersistenceConfiguration {

    @Bean
    SigningKeyRepository signingKeyRepository(final SigningKeyJpaRepository repository) {
        return new SigningKeyPersistenceAdapter(repository);
    }
}
