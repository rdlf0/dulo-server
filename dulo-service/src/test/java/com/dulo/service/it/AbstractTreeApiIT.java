package com.dulo.service.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dulo.core.shared.port.out.UnitOfWork;
import com.dulo.core.tree.port.out.TreeSettingsRepository;
import com.dulo.domain.feature.Feature;
import com.dulo.domain.shared.ConcurrentModificationException;
import com.dulo.domain.tree.TreeSettings;
import com.dulo.service.DuloApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

/**
 * Runs the full server against a real database: Liquibase migrations, the JPA adapter and the REST
 * API. Subclasses choose the database.
 */
@SpringBootTest(
        classes = DuloApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"dulo.features.map-view=false", "DULO_CONFIG_FILE=/nonexistent/dulo.yaml"})
abstract class AbstractTreeApiIT {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort int port;

    @Autowired TreeSettingsRepository trees;
    @Autowired UnitOfWork unitOfWork;

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
