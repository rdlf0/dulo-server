package com.dulo.service.it;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;

@Testcontainers
class MariaDbTreeApiIT extends AbstractTreeApiIT {

    @Container static final MariaDBContainer DB = new MariaDBContainer("mariadb:12.3");

    @DynamicPropertySource
    static void database(final DynamicPropertyRegistry registry) {
        registerDatabase(registry, DB);
    }
}
