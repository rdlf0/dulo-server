package com.dulo.service.config;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Renders the merged Dulo configuration as YAML. Every value is annotated with where it came from
 * (environment variable, config file or built-in default) and secrets are masked.
 */
public final class ConfigurationReport {

    static final String MASK = "****";
    private static final String ROOT = "dulo";

    private final ConfigurableEnvironment environment;

    public ConfigurationReport(final ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    private static String describe(final PropertySource<?> source) {
        final String name = source.getName();
        if (StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME.equals(name)) {
            return "env";
        }
        if (StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME.equals(name)) {
            return "system property";
        }
        if ("commandLineArgs".equals(name)) {
            return "command line";
        }
        if (name.contains("classpath")) {
            return "default";
        }
        if (name.contains("file [")) {
            return "file " + name.substring(name.indexOf("file [") + 6, name.indexOf(']'));
        }
        return name;
    }

    private static @Nullable Object read(final RecordComponent component, final Record record) {
        try {
            return component.getAccessor().invoke(record);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isSecret(final Class<?> type, final String field) {
        try {
            final ConfigDoc doc = type.getDeclaredField(field).getAnnotation(ConfigDoc.class);
            return doc != null && doc.secret();
        } catch (final NoSuchFieldException _) {
            return false;
        }
    }

    static String kebab(final String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    }

    public String render(final DuloProperties properties) {
        final StringBuilder out = new StringBuilder();
        out.append("# Dulo configuration (secrets masked)\n");
        out.append("# Database: ").append(properties.database().kind()).append('\n');
        out.append(ROOT).append(":\n");
        this.renderRecord(out, properties, ROOT, 1);
        return out.toString();
    }

    /** Where the value of a property comes from: env, file, default or another source name. */
    public String sourceOf(final String propertyPath) {
        final ConfigurationPropertyName name = ConfigurationPropertyName.adapt(propertyPath, '.');
        for (final ConfigurationPropertySource source :
                ConfigurationPropertySources.get(environment)) {
            final ConfigurationProperty property = source.getConfigurationProperty(name);
            if (property != null
                    && source.getUnderlyingSource() instanceof final PropertySource<?> ps) {
                return describe(ps);
            }
        }
        return "default";
    }

    private void renderRecord(
            final StringBuilder out, final Record record, final String path, final int depth) {
        for (final RecordComponent component : record.getClass().getRecordComponents()) {
            final String key = kebab(component.getName());
            final Object value = read(component, record);
            final boolean secret = isSecret(record.getClass(), component.getName());
            this.renderEntry(out, key, value, path + "." + key, depth, secret);
        }
    }

    private void renderEntry(
            final StringBuilder out,
            final String key,
            final @Nullable Object value,
            final String path,
            final int depth,
            final boolean secret) {
        final String indent = "  ".repeat(depth);
        switch (value) {
            case null -> out.append(indent).append(key).append(": ~\n");
            case final Record nested -> {
                out.append(indent).append(key).append(":\n");
                this.renderRecord(out, nested, path, depth + 1);
            }
            case final Map<?, ?> map when map.isEmpty() ->
                    out.append(indent)
                            .append(key)
                            .append(": {}")
                            .append(this.comment(path))
                            .append('\n');
            case final Map<?, ?> map -> {
                out.append(indent).append(key).append(":\n");
                new TreeMap<>(map)
                        .forEach((k, v) ->
                                this.renderEntry(
                                        out,
                                        String.valueOf(k),
                                        v,
                                        path + "." + k,
                                        depth + 1,
                                        false));
            }
            case final Collection<?> list ->
                    out.append(indent)
                            .append(key)
                            .append(": ")
                            .append(list.stream().map(String::valueOf).toList())
                            .append(this.comment(path))
                            .append('\n');
            default ->
                    out.append(indent)
                            .append(key)
                            .append(": ")
                            .append(secret ? MASK : value)
                            .append(this.comment(path))
                            .append('\n');
        }
    }

    private String comment(final String path) {
        return "  # " + this.sourceOf(path);
    }
}
