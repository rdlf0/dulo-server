package com.dulo.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dulo.domain.feature.Feature;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

class DuloPropertiesTest {

    private static DuloProperties bind(final Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bindOrCreate("dulo", DuloProperties.class);
    }

    @Test
    void appliesDefaults() {
        final DuloProperties properties =
                bind(Map.of("dulo.database.url", "jdbc:sqlite:/data/dulo.db"));

        assertThat(properties.publicUrl()).hasToString("http://localhost:8080");
        assertThat(properties.registration().mode())
                .isEqualTo(DuloProperties.RegistrationMode.APPROVAL);
        assertThat(properties.clients().web().clientId()).isEqualTo("dulo-web");
        assertThat(properties.database().kind()).isEqualTo(DuloProperties.DatabaseKind.SQLITE);
        assertThat(properties.featureOverrides().forced()).isEmpty();
        assertThat(properties.identity().providers()).isEmpty();
        assertThat(properties.clients().web().allowedOrigins()).isEmpty();
        assertThat(properties.clients().web().redirectUris()).isEmpty();
    }

    @Test
    void ignoresFeatureWithEmptyValue() {
        final DuloProperties properties =
                bind(
                        Map.of(
                                "dulo.database.url",
                                "jdbc:sqlite:/data/dulo.db",
                                "dulo.features.map-view",
                                ""));

        assertThat(properties.featureOverrides().forced()).isEmpty();
    }

    @Test
    void normalizesFeatureKeys() {
        final DuloProperties properties =
                bind(Map.of("dulo.features.map_view", "false", "dulo.features.TIMELINE", "true"));

        assertThat(properties.features()).containsEntry("map-view", false);
        assertThat(properties.featureOverrides().forced())
                .containsEntry(Feature.MAP_VIEW, false)
                .containsEntry(Feature.TIMELINE, true);
    }

    @Test
    void rejectsUnknownFeature() {
        assertThatThrownBy(() -> bind(Map.of("dulo.features.chat", "true")))
                .isInstanceOf(BindException.class)
                .rootCause()
                .hasMessageContaining("Unknown feature 'chat'");
    }

    @Test
    void rejectsAdministrationOverride() {
        assertThatThrownBy(() -> bind(Map.of("dulo.features.administration", "false")))
                .isInstanceOf(BindException.class)
                .rootCause()
                .hasMessageContaining("always enabled");
    }

    @Test
    void bindsProvidersKeyedById() {
        final DuloProperties properties =
                bind(
                        Map.of(
                                "dulo.identity.providers.pocketid.type",
                                "oidc",
                                "dulo.identity.providers.pocketid.issuer-uri",
                                "https://id.example.com",
                                "dulo.identity.providers.pocketid.client-id",
                                "dulo",
                                "dulo.identity.providers.pocketid.role-mappings.admins[0]",
                                "admin"));

        final var provider = properties.identity().providers().get("pocketid");
        assertThat(provider.type()).isEqualTo(DuloProperties.ProviderType.OIDC);
        assertThat(provider.scopes()).containsExactly("openid", "profile", "email");
        assertThat(provider.claims().roles()).isEqualTo("groups");
        assertThat(provider.roleMappings()).containsEntry("admins", List.of("admin"));
    }
}
