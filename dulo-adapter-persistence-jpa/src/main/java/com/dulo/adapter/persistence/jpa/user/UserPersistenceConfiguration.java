package com.dulo.adapter.persistence.jpa.user;

import com.dulo.core.access.port.out.UserRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UserPersistenceConfiguration {

    @Bean
    UserRepository userRepository(final UserJpaRepository repository) {
        return new UserPersistenceAdapter(repository);
    }
}
