package com.dulo.service.it;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class MySqlTreeApiIT extends AbstractTreeApiIT {

    @Container static final MySQLContainer DB = new MySQLContainer("mysql:9.7");

    @DynamicPropertySource
    static void database(final DynamicPropertyRegistry registry) {
        registerDatabase(registry, DB);
    }
}
