package com.dulo.service.wiring;

import com.dulo.core.shared.port.out.UnitOfWork;
import com.dulo.core.tree.port.out.FeatureOverridesPort;
import com.dulo.core.tree.port.out.TreeSettingsRepository;
import com.dulo.core.tree.service.TreeService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Creates the framework-free application services and connects them to their ports. */
@Configuration(proxyBeanMethods = false)
public class CoreServicesConfiguration {

    @Bean
    TreeService treeService(
            final TreeSettingsRepository trees,
            final FeatureOverridesPort featureOverrides,
            final UnitOfWork unitOfWork) {
        return new TreeService(trees, featureOverrides, unitOfWork);
    }
}
