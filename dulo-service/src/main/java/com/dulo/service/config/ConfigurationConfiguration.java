package com.dulo.service.config;

import com.dulo.core.identity.port.out.IdentityProvidersPort;
import com.dulo.core.tree.port.in.GetTreeUseCase;
import com.dulo.core.tree.port.out.FeatureOverridesPort;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DuloProperties.class)
public class ConfigurationConfiguration {

    @Bean
    FeatureOverridesPort featureOverridesPort(final DuloProperties properties) {
        final var overrides = properties.featureOverrides();
        return () -> overrides;
    }

    @Bean
    IdentityProvidersPort identityProvidersPort(final DuloProperties properties) {
        return new ConfiguredIdentityProviders(properties);
    }

    @Bean
    ConfigurationReport configurationReport(final ConfigurableEnvironment environment) {
        return new ConfigurationReport(environment);
    }

    @Bean
    ConfigurationReportLogger configurationReportLogger(
            final ConfigurationReport report,
            final DuloProperties properties,
            final GetTreeUseCase getTree) {
        return new ConfigurationReportLogger(report, properties, getTree);
    }
}
