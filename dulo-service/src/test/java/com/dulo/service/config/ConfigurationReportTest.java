package com.dulo.service.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

class ConfigurationReportTest {

    @Test
    void annotatesSourcesAndMasksSecrets() {
        final StandardEnvironment environment = new StandardEnvironment();
        final var sources = environment.getPropertySources();
        sources.replace(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        Map.of(
                                "DULO_DATABASE_PASSWORD", "s3cret",
                                "DULO_FEATURES_MAP_VIEW", "false",
                                "DULO_IDENTITY_PROVIDERS_GOOGLE_TYPE", "google",
                                "DULO_IDENTITY_PROVIDERS_GOOGLE_CLIENTID", "abc",
                                "DULO_IDENTITY_PROVIDERS_GOOGLE_CLIENTSECRET", "google-secret")));
        sources.addLast(
                new MapPropertySource(
                        "Config resource 'file [/config/dulo.yaml]' via location 'optional:file:/config/dulo.yaml'",
                        Map.of("dulo.database.url", "jdbc:postgresql://db:5432/dulo")));

        final DuloProperties properties =
                Binder.get(environment).bind("dulo", DuloProperties.class).get();
        final String report = new ConfigurationReport(environment).render(properties);

        assertThat(report)
                .contains("# Database: POSTGRESQL")
                .contains("url: jdbc:postgresql://db:5432/dulo  # file /config/dulo.yaml")
                .contains("password: ****  # env")
                .contains("map-view: false  # env")
                .contains("client-id: abc  # env")
                .contains("client-secret: ****  # env")
                .contains("print-config: false  # default")
                .doesNotContain("s3cret")
                .doesNotContain("google-secret");
    }

    @Test
    void convertsComponentNamesToKebabCase() {
        assertThat(ConfigurationReport.kebab("clientSecret")).isEqualTo("client-secret");
        assertThat(ConfigurationReport.kebab("dataDir")).isEqualTo("data-dir");
        assertThat(ConfigurationReport.kebab("url")).isEqualTo("url");
    }
}
