package com.dulo.service.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.dulo.core.access.port.out.RoleRepository;
import com.dulo.core.access.port.out.UserRepository;
import com.dulo.core.identity.port.in.ExternalSignIn;
import com.dulo.core.identity.port.in.SignInUseCase;
import com.dulo.core.identity.port.in.SigningKeyUseCase;
import com.dulo.core.shared.port.out.UnitOfWork;
import com.dulo.core.tree.port.out.TreeSettingsRepository;
import com.dulo.domain.access.Access;
import com.dulo.domain.access.Permissions;
import com.dulo.domain.access.Role;
import com.dulo.domain.access.RoleKey;
import com.dulo.domain.feature.Feature;
import com.dulo.domain.identity.SigningKey;
import com.dulo.domain.shared.ConcurrentModificationException;
import com.dulo.domain.tree.TreeSettings;
import com.dulo.domain.user.ExternalIdentity;
import com.dulo.domain.user.User;
import com.dulo.domain.user.UserId;
import com.dulo.domain.user.UserStatus;
import com.dulo.service.DuloApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.JdbcDatabaseContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

/**
 * Runs the full server against a real database: Liquibase migrations, the JPA adapter and the REST
 * API. Subclasses choose the database. All tests share one database, so each test uses its own
 * data.
 */
@SpringBootTest(
        classes = DuloApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "dulo.features.map-view=false",
            "DULO_CONFIG_FILE=/nonexistent/dulo.yaml",
            "dulo.identity.providers.mock.type=oidc",
            "dulo.identity.providers.mock.issuer-uri=http://localhost/mock",
            "dulo.identity.providers.mock.client-id=dulo",
            "dulo.identity.providers.mock.role-mappings[family-admins]=admin"
        })
abstract class AbstractApiIT {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort int port;

    @Autowired TreeSettingsRepository trees;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired SignInUseCase signIn;
    @Autowired SigningKeyUseCase signingKeys;
    @Autowired UnitOfWork unitOfWork;

    /**
     * Points the server at a database container. Call it from each subclass's
     * {@code @DynamicPropertySource} method.
     *
     * @param registry the test's dynamic properties
     * @param db the database container
     */
    static void registerDatabase(
            final DynamicPropertyRegistry registry, final JdbcDatabaseContainer<?> db) {
        registry.add("dulo.database.url", db::getJdbcUrl);
        registry.add("dulo.database.username", db::getUsername);
        registry.add("dulo.database.password", db::getPassword);
    }

    private static JsonNode feature(final JsonNode tree, final String key) {
        for (final JsonNode feature : tree.path("features")) {
            if (feature.path("key").asString().equals(key)) {
                return feature;
            }
        }
        throw new AssertionError("Feature " + key + " missing in " + tree);
    }

    @Test
    void treeLifecycle() throws Exception {
        // Before setup there is no tree.
        final HttpResponse<String> missing = this.get("/api/v1/tree");
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(missing.headers().firstValue("Content-Type"))
                .hasValueSatisfying(type -> assertThat(type).contains("application/problem+json"));
        assertThat(JSON.readTree(missing.body()).path("type").asString())
                .isEqualTo("urn:dulo:problem:tree-not-initialized");

        // Store a tree through the outbound port, the way the setup flow will.
        final TreeSettings created =
                TreeSettings.create(
                        "The Smiths", "Since 1820", Set.of(Feature.PEOPLE, Feature.MAP_VIEW));
        final TreeSettings stored = unitOfWork.write(() -> trees.save(created));
        assertThat(stored.version()).isZero();

        final HttpResponse<String> found = this.get("/api/v1/tree");
        assertThat(found.statusCode()).isEqualTo(200);
        assertThat(found.headers().firstValue("ETag")).contains("\"0\"");
        final JsonNode body = JSON.readTree(found.body());
        assertThat(body.path("id").asString()).isEqualTo(created.id().toString());
        assertThat(body.path("name").asString()).isEqualTo("The Smiths");
        assertThat(feature(body, "people").path("enabled").asBoolean()).isTrue();
        assertThat(feature(body, "people").path("locked").asBoolean()).isFalse();
        // Enabled in the database, but forced off by configuration.
        assertThat(feature(body, "map-view").path("enabled").asBoolean()).isFalse();
        assertThat(feature(body, "map-view").path("locked").asBoolean()).isTrue();

        // Updates bump the version; stale updates are rejected.
        final TreeSettings renamed =
                new TreeSettings(
                        stored.id(),
                        "Smith family",
                        null,
                        stored.enabledFeatures(),
                        stored.version());
        assertThat(unitOfWork.write(() -> trees.save(renamed)).version()).isEqualTo(1);
        assertThatThrownBy(() -> unitOfWork.write(() -> trees.save(renamed)))
                .isInstanceOf(ConcurrentModificationException.class);
        assertThat(unitOfWork.read(trees::find))
                .hasValueSatisfying(tree -> assertThat(tree.name()).isEqualTo("Smith family"));
    }

    @Test
    void rolesRoundTrip() {
        final RoleKey key = new RoleKey("it-readers");
        final Permissions viewPeople = new Permissions(Map.of(Feature.PEOPLE, Set.of(Access.VIEW)));
        final Role stored =
                unitOfWork.write(() -> roles.save(new Role(key, "Readers", false, viewPeople, 0)));
        assertThat(stored.version()).isZero();
        assertThat(unitOfWork.read(() -> roles.find(key))).contains(stored);

        final Permissions viewAndEdit =
                viewPeople.union(new Permissions(Map.of(Feature.PLACES, Set.of(Access.EDIT))));
        final Role changed = new Role(key, "Editors", false, viewAndEdit, stored.version());
        final Role updated = unitOfWork.write(() -> roles.save(changed));
        assertThat(updated.version()).isEqualTo(1);
        assertThat(unitOfWork.read(() -> roles.find(key))).contains(updated);
        assertThatThrownBy(() -> unitOfWork.write(() -> roles.save(changed)))
                .isInstanceOf(ConcurrentModificationException.class);

        assertThat(unitOfWork.read(() -> roles.findAllByKeys(Set.of(key, new RoleKey("ghost")))))
                .containsExactly(updated);
        assertThat(unitOfWork.read(roles::findAll)).contains(updated);
    }

    @Test
    void usersRoundTrip() {
        final ExternalIdentity identity = new ExternalIdentity("mock", "it-ada");
        final UserId id =
                signIn.signIn(
                        new ExternalSignIn(
                                identity, "ada@example.com", "Ada", Set.of("family-admins")));

        final User user = unitOfWork.read(() -> users.findById(id)).orElseThrow();
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.claimRoles()).containsExactly(new RoleKey("admin"));
        assertThat(user.email()).isEqualTo("ada@example.com");
        assertThat(user.lastSignIn()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(user.version()).isZero();

        // Manual roles and the status survive the next sign-in; claim roles are recomputed.
        final User changed =
                user.withManualRoles(Set.of(new RoleKey("editor"))).withStatus(UserStatus.DISABLED);
        assertThat(unitOfWork.write(() -> users.save(changed)).version()).isEqualTo(1);
        assertThat(signIn.signIn(new ExternalSignIn(identity, null, "Ada L.", Set.of())))
                .isEqualTo(id);

        final User reloaded =
                unitOfWork.read(() -> users.findByExternalIdentity(identity)).orElseThrow();
        assertThat(reloaded.id()).isEqualTo(id);
        assertThat(reloaded.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(reloaded.claimRoles()).isEmpty();
        assertThat(reloaded.manualRoles()).containsExactly(new RoleKey("editor"));
        assertThat(reloaded.email()).isNull();
        assertThat(reloaded.displayName()).isEqualTo("Ada L.");
        assertThatThrownBy(() -> unitOfWork.write(() -> users.save(changed)))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void signingKeyIsCreatedOnce() {
        // Longer than any varchar, to check the column holds a whole key.
        final String material = "{\"k\":\"%s\"}".formatted("x".repeat(20_000));

        final SigningKey first =
                signingKeys.currentOrCreate(
                        () -> new SigningKey("it-key", material, Instant.now()));
        final SigningKey second =
                signingKeys.currentOrCreate(() -> {
                    throw new AssertionError("A key exists already");
                });

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.material()).isEqualTo(first.material());
        assertThat(second.createdAt()).isCloseTo(first.createdAt(), within(1, ChronoUnit.SECONDS));
    }

    @Test
    void healthIsUp() throws Exception {
        final HttpResponse<String> health = this.get("/actuator/health/readiness");

        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(health.body()).path("status").asString()).isEqualTo("UP");
    }

    private HttpResponse<String> get(final String path) throws IOException, InterruptedException {
        return HTTP.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
