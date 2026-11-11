package com.dulo.service.config;

import com.dulo.domain.feature.Feature;
import com.dulo.domain.feature.FeatureOverrides;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * All Dulo settings. Values come from {@code application.yaml} defaults, an optional YAML file
 * ({@code DULO_CONFIG_FILE}, default {@code /config/dulo.yaml}) and environment variables, in
 * increasing order of precedence.
 */
@ConfigurationProperties("dulo")
@Validated
public record DuloProperties(
        @ConfigDoc("Public base URL of the server, used in redirects and issued tokens.")
                @DefaultValue("http://localhost:8080")
                URI publicUrl,
        @ConfigDoc("Directory for local data such as the SQLite database.") @DefaultValue("./data")
                Path dataDir,
        @ConfigDoc("Database connection.") @Valid @DefaultValue Database database,
        @ConfigDoc("Identity providers that users sign in with.") @Valid @DefaultValue
                Identity identity,
        @ConfigDoc("OAuth2 clients allowed to call the API.") @Valid @DefaultValue Clients clients,
        @ConfigDoc("What happens when an unknown user signs in for the first time.")
                @Valid
                @DefaultValue
                Registration registration,
        @ConfigDoc("Initial setup.") @Valid @DefaultValue Setup setup,
        @ConfigDoc(
                        "Forces features on (true) or off (false). Forced features are locked in"
                                + " the administration page. Features left out are managed by the"
                                + " administrator.")
                @DefaultValue
                Map<String, Boolean> features,
        @ConfigDoc("Print the merged configuration on every start, not only before setup.")
                @DefaultValue("false")
                boolean printConfig) {

    public DuloProperties {
        features = normalizeFeatureKeys(features);
    }

    private static Map<String, Boolean> normalizeFeatureKeys(final Map<String, Boolean> features) {
        final Map<String, Boolean> normalized = new LinkedHashMap<>();
        features.forEach((key, enabled) -> {
            final Feature feature =
                    Feature.fromKey(key)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Unknown feature '%s' in dulo.features. Known features: %s"
                                                    .formatted(key, toggleableFeatureKeys())));
            if (!feature.toggleable()) {
                throw new IllegalArgumentException(
                        "Feature '%s' is always enabled and cannot be configured"
                                .formatted(feature.key()));
            }
            normalized.put(feature.key(), enabled);
        });
        return Map.copyOf(normalized);
    }

    static List<String> toggleableFeatureKeys() {
        return Arrays.stream(Feature.values())
                .filter(Feature::toggleable)
                .map(Feature::key)
                .toList();
    }

    /** Feature overrides as a domain object. Validates the feature keys. */
    public FeatureOverrides featureOverrides() {
        final Map<Feature, Boolean> forced = new EnumMap<>(Feature.class);
        features.forEach((key, enabled) -> forced.put(Feature.fromKey(key).orElseThrow(), enabled));
        return new FeatureOverrides(forced);
    }

    public enum DatabaseKind {
        SQLITE("jdbc:sqlite:"),
        POSTGRESQL("jdbc:postgresql:"),
        MYSQL("jdbc:mysql:"),
        MARIADB("jdbc:mariadb:"),
        UNSUPPORTED(""),
        ;

        private final String prefix;

        DatabaseKind(final String prefix) {
            this.prefix = prefix;
        }

        static DatabaseKind fromJdbcUrl(final String url) {
            for (final DatabaseKind kind : values()) {
                if (kind != UNSUPPORTED && url.startsWith(kind.prefix)) {
                    return kind;
                }
            }
            return UNSUPPORTED;
        }
    }

    public enum ProviderType {
        OIDC,
        GOOGLE,
        MICROSOFT,
        ;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public enum RegistrationMode {
        AUTO,
        APPROVAL,
        ;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public record Database(
            @ConfigDoc(
                            "JDBC URL. Defaults to a SQLite file in the data directory. Examples:"
                                    + " jdbc:postgresql://db:5432/dulo, jdbc:mysql://db:3306/dulo,"
                                    + " jdbc:mariadb://db:3306/dulo.")
                    @NotBlank
                    String url,
            @ConfigDoc("Database user. Not used by SQLite.") @Nullable String username,
            @ConfigDoc(value = "Database password. Not used by SQLite.", secret = true)
                    @Nullable String password) {

        /** The database product, derived from the JDBC URL. */
        public DatabaseKind kind() {
            return DatabaseKind.fromJdbcUrl(url);
        }

        @AssertTrue(
                message =
                        "dulo.database.url must be a SQLite, PostgreSQL, MySQL or MariaDB JDBC URL")
        boolean isSupported() {
            return this.kind() != DatabaseKind.UNSUPPORTED;
        }
    }

    public record Identity(
            @ConfigDoc(
                            "Identity providers keyed by a short id of lowercase letters and"
                                    + " digits, e.g. google or pocketid. The id is part of the"
                                    + " environment variable names, e.g."
                                    + " DULO_IDENTITY_PROVIDERS_GOOGLE_CLIENTID.")
                    @DefaultValue
                    Map<String, @Valid Provider> providers) {

        public Identity {
            providers = Map.copyOf(providers);
            providers
                    .keySet()
                    .forEach(id -> {
                        if (!id.matches("[a-z0-9]+")) {
                            throw new IllegalArgumentException(
                                    "Identity provider id '%s' must contain only lowercase letters and digits"
                                            .formatted(id));
                        }
                    });
        }
    }

    public record Provider(
            @ConfigDoc("Provider type.") @NotNull ProviderType type,
            @ConfigDoc("Name shown on the sign-in page.") @Nullable String displayName,
            @ConfigDoc(
                            "OIDC issuer URL. Required for oidc and microsoft, e.g."
                                    + " https://login.microsoftonline.com/<tenant>/v2.0.")
                    @Nullable URI issuerUri,
            @ConfigDoc("OAuth2 client id registered with the provider.") @NotBlank String clientId,
            @ConfigDoc(value = "OAuth2 client secret registered with the provider.", secret = true)
                    @Nullable String clientSecret,
            @ConfigDoc("Requested scopes.") @DefaultValue({"openid", "profile", "email"})
                    List<String> scopes,
            @ConfigDoc("Names of the claims Dulo reads from the provider's ID token.")
                    @Valid
                    @DefaultValue
                    Claims claims,
            @ConfigDoc(
                            "Maps values of the roles claim to Dulo roles, e.g. {dulo-admins:"
                                    + " [admin]}.")
                    @DefaultValue
                    Map<String, List<String>> roleMappings,
            @ConfigDoc("Dulo role for users whose claims match no role mapping.")
                    @Nullable String defaultRole) {

        public Provider {
            roleMappings = Map.copyOf(roleMappings);
        }

        @AssertTrue(message = "issuer-uri is required for oidc and microsoft providers")
        boolean isIssuerUriPresent() {
            return type != ProviderType.OIDC && type != ProviderType.MICROSOFT || issuerUri != null;
        }
    }

    public record Claims(
            @ConfigDoc("Claim holding the user's groups or roles.") @DefaultValue("groups")
                    String roles,
            @ConfigDoc("Claim holding the email address.") @DefaultValue("email") String email,
            @ConfigDoc("Claim holding the display name.") @DefaultValue("name") String name) {}

    public record Clients(@ConfigDoc("The Dulo web client.") @Valid @DefaultValue WebClient web) {}

    public record WebClient(
            @ConfigDoc("OAuth2 client id of the web client.") @DefaultValue("dulo-web")
                    String clientId,
            @ConfigDoc("Allowed redirect URIs after sign-in.") @DefaultValue List<URI> redirectUris,
            @ConfigDoc("Origins allowed to call the API from a browser (CORS).") @DefaultValue
                    List<String> allowedOrigins) {

        public WebClient {
            redirectUris = List.copyOf(redirectUris);
            allowedOrigins = List.copyOf(allowedOrigins);
        }
    }

    public record Registration(
            @ConfigDoc(
                            "auto: new users get access right away. approval: an administrator"
                                    + " must approve them first.")
                    @DefaultValue("approval")
                    RegistrationMode mode) {}

    public record Setup(
            @ConfigDoc(
                            value =
                                    "One-time token that authorizes the initial setup. Generated"
                                            + " and printed on start when not set.",
                            secret = true)
                    @Nullable String token) {}
}
