package com.dulo.service.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.dulo.service.DuloApplication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Keeps {@code api/openapi.json} in sync with the REST API. Clients generate their code from that
 * file, and the docs site shows it. When the API changes, regenerate it with the command in {@link
 * #REGENERATE}.
 */
@SpringBootTest(
        classes = DuloApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "DULO_CONFIG_FILE=/nonexistent/dulo.yaml")
class OpenApiDocumentTest {

    /** Regenerates the OpenAPI document. */
    static final String REGENERATE =
            "./mvnw -pl dulo-service -am test -Dtest=OpenApiDocumentTest"
                    + " -Dsurefire.failIfNoSpecifiedTests=false -Ddulo.openapi.update=true";

    @TempDir static Path dataDir;

    private static final Path DOCUMENT = Path.of("..", "api", "openapi.json");

    private static final JsonMapper JSON =
            JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    @LocalServerPort int port;

    @DynamicPropertySource
    static void database(final DynamicPropertyRegistry registry) {
        registry.add("dulo.data-dir", dataDir::toString);
    }

    @Test
    void documentIsUpToDate() throws IOException, InterruptedException {
        final HttpResponse<String> response =
                HttpClient.newHttpClient()
                        .send(
                                HttpRequest.newBuilder(
                                                URI.create(
                                                        "http://localhost:"
                                                                + this.port
                                                                + "/api/v1/openapi.json"))
                                        .build(),
                                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        final String generated = JSON.writeValueAsString(JSON.readTree(response.body())) + "\n";

        if (Boolean.getBoolean("dulo.openapi.update") || !Files.exists(DOCUMENT)) {
            Files.createDirectories(DOCUMENT.getParent());
            Files.writeString(DOCUMENT, generated);
        }

        assertThat(Files.readString(DOCUMENT))
                .as("api/openapi.json is outdated. Regenerate it with: " + REGENERATE)
                .isEqualTo(generated);
    }
}
