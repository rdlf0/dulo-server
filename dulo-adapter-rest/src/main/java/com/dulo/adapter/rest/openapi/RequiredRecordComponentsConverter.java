package com.dulo.adapter.rest.openapi;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.RecordComponent;
import java.util.Iterator;

/**
 * Marks the properties of record schemas as required, except for components annotated with
 * JSpecify's {@link Nullable}. The API records are {@code @NullMarked}, so this matches their
 * contract, and generated clients get non-optional fields.
 */
final class RequiredRecordComponentsConverter implements ModelConverter {

    private static @Nullable Schema<?> definition(
            final @Nullable Schema<?> schema, final ModelConverterContext context) {
        if (schema == null || schema.get$ref() == null) {
            return schema;
        }
        final String ref = schema.get$ref();
        return context.getDefinedModels().get(ref.substring(ref.lastIndexOf('/') + 1));
    }

    @Override
    public @Nullable Schema<?> resolve(
            final AnnotatedType type,
            final ModelConverterContext context,
            final Iterator<ModelConverter> chain) {
        if (!chain.hasNext()) {
            return null;
        }
        final Schema<?> schema = chain.next().resolve(type, context, chain);
        final Class<?> raw = Json.mapper().constructType(type.getType()).getRawClass();
        final Schema<?> model = definition(schema, context);
        if (!raw.isRecord() || model == null || model.getProperties() == null) {
            return schema;
        }
        for (final RecordComponent component : raw.getRecordComponents()) {
            final String name = component.getName();
            if (model.getProperties().containsKey(name)
                    && !component.getAnnotatedType().isAnnotationPresent(Nullable.class)
                    && (model.getRequired() == null || !model.getRequired().contains(name))) {
                model.addRequiredItem(name);
            }
        }
        return schema;
    }
}
