package com.dulo.service.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Describes a configuration property. Used to generate the JSON Schema of the YAML config. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigDoc {

    String value();

    /** Marks values that the configuration report masks. */
    boolean secret() default false;
}
