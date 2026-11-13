package com.dulo.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dulo.domain.access.RoleKey;
import com.dulo.domain.identity.RegistrationMode;
import com.dulo.domain.identity.RoleMapping;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class ConfiguredIdentityProvidersTest {

    private static final String POCKETID = "dulo.identity.providers.pocketid.";

    private static ConfiguredIdentityProviders providers(final Map<String, String> values) {
        final Map<String, String> all = new HashMap<>(values);
        all.put(POCKETID + "type", "oidc");
        all.put(POCKETID + "issuer-uri", "https://id.example.com");
        all.put(POCKETID + "client-id", "dulo");
        return new ConfiguredIdentityProviders(
                new Binder(new MapConfigurationPropertySource(all))
                        .bindOrCreate("dulo", DuloProperties.class));
    }

    @Test
    void mapsRoleMappingsAndDefaultRole() {
        final ConfiguredIdentityProviders providers =
                providers(
                        Map.of(
                                POCKETID + "role-mappings[family-admins][0]", "admin",
                                POCKETID + "role-mappings[family-admins][1]", "editor",
                                POCKETID + "default-role", "viewer"));

        final RoleMapping mapping = providers.roleMapping("pocketid").orElseThrow();

        assertThat(mapping.mappedRoles(Set.of("family-admins")))
                .containsExactlyInAnyOrder(new RoleKey("admin"), new RoleKey("editor"));
        assertThat(mapping.defaultRoleIfAny()).contains(new RoleKey("viewer"));
        assertThat(providers.roleMapping("google")).isEmpty();
    }

    @Test
    void providerWithoutMappingsMapsNothing() {
        assertThat(providers(Map.of()).roleMapping("pocketid")).contains(RoleMapping.none());
    }

    @Test
    void mapsTheRegistrationMode() {
        assertThat(providers(Map.of()).registrationMode()).isEqualTo(RegistrationMode.APPROVAL);
        assertThat(providers(Map.of("dulo.registration.mode", "auto")).registrationMode())
                .isEqualTo(RegistrationMode.AUTO);
    }

    @Test
    void rejectsInvalidRoleKeys() {
        assertThatThrownBy(() -> providers(Map.of(POCKETID + "default-role", "Viewers")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dulo.identity.providers.pocketid")
                .hasMessageContaining("Viewers");
    }
}
