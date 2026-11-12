package com.dulo.service.it;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class PostgresTreeApiIT extends AbstractTreeApiIT {

    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine");

    @DynamicPropertySource
    static void database(final DynamicPropertyRegistry registry) {
        registerDatabase(registry, DB);
    }
}
