package com.dulo.service.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.dulo.domain.feature.Feature;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.networknt.schema.InputFormat;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.DefaultValue;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Keeps {@code schema/dulo-config.schema.json} in sync with {@link DuloProperties}. When the
 * properties change, regenerate the schema with the command in {@link #REGENERATE}.
 */
class ConfigSchemaTest {

    /** Regenerates the schema file. */
    static final String REGENERATE =
            "./mvnw -pl dulo-service -am test -Dtest=ConfigSchemaTest"
                    + " -Dsurefire.failIfNoSpecifiedTests=false -Ddulo.schema.update=true";

    private static final Path SCHEMA = Path.of("..", "schema", "dulo-config.schema.json");
    private static final Path EXAMPLE = Path.of("..", "deploy", "dulo.example.yaml");
    private static final String SCHEMA_ID =
            "https://raw.githubusercontent.com/rdlf0/dulo-server/main/schema/dulo-config.schema.json";

    private static final JsonMapper JSON =
            JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private static com.networknt.schema.Schema compiledSchema() {
        return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(JSON.writeValueAsString(generate()), InputFormat.JSON);
    }

    static ObjectNode generate() {
        final var builder =
                new SchemaGeneratorConfigBuilder(
                                JSON, SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                        .with(Option.FLATTENED_ENUMS_FROM_TOSTRING)
                        .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                        .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT);
        builder.forTypesInGeneral()
                .withCustomDefinitionProvider((javaType, context) -> {
                    final Class<?> type = javaType.getErasedType();
                    if (type == Path.class || type == URI.class) {
                        final ObjectNode node = context.getGeneratorConfig().createObjectNode();
                        node.put("type", "string");
                        return new CustomDefinition(node);
                    }
                    return null;
                });
        builder.forFields()
                .withPropertyNameOverrideResolver(
                        field -> ConfigurationReport.kebab(field.getDeclaredName()))
                .withDescriptionResolver(field -> {
                    final ConfigDoc doc = field.getAnnotation(ConfigDoc.class);
                    return doc == null ? null : doc.value();
                })
                .withRequiredCheck(field ->
                        field.getAnnotation(NotBlank.class) != null
                                || field.getAnnotation(NotNull.class) != null)
                .withDefaultResolver(ConfigSchemaTest::defaultValue);

        final ObjectNode schema =
                new SchemaGenerator(builder.build()).generateSchema(DuloConfigFile.class);
        schema.put("$id", SCHEMA_ID);
        schema.put("title", "Dulo server configuration");
        // Other keys (spring, server, logging, ...) may appear next to dulo.
        schema.remove("additionalProperties");
        // The database URL has a built-in default, so the config file never needs it.
        removeRequired(schema, "Database", "url");
        replaceFeatures(schema);
        return schema;
    }

    private static Object defaultValue(final FieldScope field) {
        final var component =
                Arrays.stream(field.getDeclaringType().getErasedType().getRecordComponents())
                        .filter(c -> c.getName().equals(field.getDeclaredName()))
                        .findFirst();
        final String[] values =
                component
                        .map(c -> c.getAnnotation(DefaultValue.class))
                        .map(DefaultValue::value)
                        .orElse(new String[0]);
        if (values.length == 0) {
            return null;
        }
        final Class<?> type = field.getType().getErasedType();
        if (List.class.isAssignableFrom(type)) {
            return List.of(values);
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(values[0]);
        }
        return values[0];
    }

    private static void removeRequired(
            final ObjectNode schema, final String definition, final String property) {
        if (schema.path("$defs").path(definition).get("required")
                instanceof final ArrayNode required) {
            for (int i = required.size() - 1; i >= 0; i--) {
                if (required.get(i).asString().equals(property)) {
                    required.remove(i);
                }
            }
            if (required.isEmpty()) {
                ((ObjectNode) schema.path("$defs").path(definition)).remove("required");
            }
        }
    }

    private static void replaceFeatures(final ObjectNode schema) {
        final ObjectNode features = JSON.createObjectNode();
        features.put("type", "object");
        features.put(
                "description",
                "Forces features on (true) or off (false). Forced features are locked in the"
                        + " administration page. Features left out are managed by the"
                        + " administrator.");
        final ObjectNode properties = features.putObject("properties");
        Arrays.stream(Feature.values())
                .filter(Feature::toggleable)
                .forEach(f -> properties.putObject(f.key()).put("type", "boolean"));
        features.put("additionalProperties", false);

        final ObjectNode duloProperties = findDuloProperties(schema);
        duloProperties.set("features", features);
    }

    private static ObjectNode findDuloProperties(final ObjectNode schema) {
        JsonNode dulo = schema.path("properties").path("dulo");
        if (dulo.has("$ref")) {
            final String ref = dulo.get("$ref").asString();
            dulo = schema.path("$defs").path(ref.substring(ref.lastIndexOf('/') + 1));
        }
        return (ObjectNode) dulo.path("properties");
    }

    @Test
    void schemaIsUpToDate() throws IOException {
        final String generated = JSON.writeValueAsString(generate()) + "\n";

        if (Boolean.getBoolean("dulo.schema.update") || !Files.exists(SCHEMA)) {
            Files.createDirectories(SCHEMA.getParent());
            Files.writeString(SCHEMA, generated);
        }

        assertThat(Files.readString(SCHEMA))
                .as("schema/dulo-config.schema.json is outdated. Regenerate it with: " + REGENERATE)
                .isEqualTo(generated);
    }

    @Test
    void exampleConfigMatchesSchema() throws IOException {
        final var schema = compiledSchema();
        final JsonNode example = new YAMLMapper().readTree(Files.readString(EXAMPLE));

        assertThat(schema.validate(example)).isEmpty();
    }

    @Test
    void schemaRejectsTypos() throws IOException {
        final var schema = compiledSchema();
        final JsonNode config =
                new YAMLMapper().readTree("dulo:\n  databse:\n    url: jdbc:sqlite:x.db\n");

        assertThat(schema.validate(config)).isNotEmpty();
    }

    /** Shape of the YAML config file: everything lives under the {@code dulo} key. */
    record DuloConfigFile(@ConfigDoc("Dulo settings.") DuloProperties dulo) {}
}
