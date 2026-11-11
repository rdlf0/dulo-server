package com.dulo.service.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Verifies that environment variables win over the YAML file, which wins over defaults. */
class ConfigPrecedenceTest {

    @TempDir Path tempDir;

    private static ConfigurableApplicationContext start(final Map<String, String> env) {
        final StandardEnvironment environment = new StandardEnvironment();
        final Map<String, Object> variables = new HashMap<>(env);
        environment
                .getPropertySources()
                .replace(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        new SystemEnvironmentPropertySource(
                                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                                variables));
        return new SpringApplicationBuilder(PropertiesOnly.class)
                .environment(environment)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .run();
    }

    @Test
    void environmentOverridesFileWhichOverridesDefaults() throws IOException {
        final Path configFile = tempDir.resolve("dulo.yaml");
        Files.writeString(
                configFile,
                """
                dulo:
                  public-url: https://tree.example.com
                  database:
                    url: jdbc:postgresql://from-file:5432/dulo
                """);

        try (final var context =
                start(
                        Map.of(
                                "DULO_CONFIG_FILE",
                                configFile.toString(),
                                "DULO_DATABASE_URL",
                                "jdbc:mysql://from-env:3306/dulo"))) {
            final DuloProperties properties = context.getBean(DuloProperties.class);
            final ConfigurationReport report = new ConfigurationReport(context.getEnvironment());

            assertThat(properties.database().url()).isEqualTo("jdbc:mysql://from-env:3306/dulo");
            assertThat(report.sourceOf("dulo.database.url")).isEqualTo("env");

            assertThat(properties.publicUrl()).hasToString("https://tree.example.com");
            assertThat(report.sourceOf("dulo.public-url")).startsWith("file ");

            assertThat(properties.dataDir()).isEqualTo(Path.of("data"));
            assertThat(report.sourceOf("dulo.data-dir")).isEqualTo("default");
        }
    }

    @Test
    void sqliteInDataDirIsTheDefaultDatabase() {
        try (final var context =
                start(
                        Map.of(
                                "DULO_CONFIG_FILE",
                                tempDir.resolve("missing.yaml").toString(),
                                "DULO_DATA_DIR",
                                "/srv/dulo"))) {
            final DuloProperties properties = context.getBean(DuloProperties.class);

            assertThat(properties.database().url()).isEqualTo("jdbc:sqlite:/srv/dulo/dulo.db");
            assertThat(properties.database().kind()).isEqualTo(DuloProperties.DatabaseKind.SQLITE);
        }
    }

    @Test
    void exampleConfigFileBinds() {
        final Path example =
                Path.of("..", "deploy", "dulo.example.yaml").toAbsolutePath().normalize();

        try (final var context = start(Map.of("DULO_CONFIG_FILE", example.toString()))) {
            final DuloProperties properties = context.getBean(DuloProperties.class);

            assertThat(properties.identity().providers()).isNotEmpty();
            assertThat(properties.featureOverrides().forced()).isNotEmpty();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DuloProperties.class)
    static class PropertiesOnly {}
}
