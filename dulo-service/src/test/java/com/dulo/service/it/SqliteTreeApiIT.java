package com.dulo.service.it;

import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

class SqliteTreeApiIT extends AbstractTreeApiIT {

    @TempDir static Path dataDir;

    @DynamicPropertySource
    static void database(final DynamicPropertyRegistry registry) {
        // Nested directory checks that the SQLite directory is created on start.
        registry.add("dulo.data-dir", () -> dataDir.resolve("nested").toString());
    }
}
