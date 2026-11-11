package com.dulo.adapter.rest;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/** Entry point of the REST adapter. Import it to expose the API under {@code /api/v1}. */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = RestApiConfiguration.class)
public class RestApiConfiguration {}
