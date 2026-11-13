package com.dulo.adapter.persistence.jpa.role;

import com.dulo.core.access.port.out.RoleRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RolePersistenceConfiguration {

    @Bean
    RoleRepository roleRepository(final RoleJpaRepository repository) {
        return new RolePersistenceAdapter(repository);
    }
}
