package com.dulo.service;

import com.dulo.adapter.persistence.jpa.PersistenceJpaConfiguration;
import com.dulo.adapter.rest.RestApiConfiguration;
import com.dulo.service.config.ConfigurationConfiguration;
import com.dulo.service.wiring.CoreServicesConfiguration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** Starts the Dulo server. Adapters are imported explicitly so the wiring stays visible. */
@SpringBootApplication(proxyBeanMethods = false)
@Import({
    ConfigurationConfiguration.class,
    CoreServicesConfiguration.class,
    PersistenceJpaConfiguration.class,
    RestApiConfiguration.class
})
public final class DuloApplication {

    private DuloApplication() {}

    static void main(final String[] args) {
        SpringApplication.run(DuloApplication.class, args);
    }
}
