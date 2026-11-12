package com.dulo.adapter.rest.openapi;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Header and schema rules of the generated OpenAPI document. */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    /**
     * Version of the API contract, matching the {@code /api/v1} path. It is not the server version,
     * so the published OpenAPI document only changes when the API does.
     */
    static final String API_VERSION = "1";

    /**
     * Describes the API. The server URL is a variable, so clients and the Swagger UI can point it
     * at any Dulo server.
     *
     * @return the OpenAPI document header
     */
    @Bean
    OpenAPI duloOpenApi() {
        final ServerVariable baseUrl =
                new ServerVariable()
                        ._default("http://localhost:8080")
                        .description("Address of your Dulo server");
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Dulo API")
                                .version(API_VERSION)
                                .description("REST API of the Dulo family tree server."))
                .servers(
                        List.of(
                                new Server()
                                        .url("{baseUrl}")
                                        .description("Dulo server")
                                        .variables(
                                                new ServerVariables()
                                                        .addServerVariable("baseUrl", baseUrl))));
    }

    /**
     * Marks record components as required unless they are {@code @Nullable}.
     *
     * @return the model converter
     */
    @Bean
    ModelConverter requiredRecordComponents() {
        return new RequiredRecordComponentsConverter();
    }
}
