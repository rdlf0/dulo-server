package com.dulo.service.wiring;

import com.dulo.core.access.port.out.UserRepository;
import com.dulo.core.identity.port.out.IdentityProvidersPort;
import com.dulo.core.identity.port.out.SigningKeyRepository;
import com.dulo.core.identity.service.SignInService;
import com.dulo.core.identity.service.SigningKeyService;
import com.dulo.core.shared.port.out.UnitOfWork;
import com.dulo.core.tree.port.out.FeatureOverridesPort;
import com.dulo.core.tree.port.out.TreeSettingsRepository;
import com.dulo.core.tree.service.TreeService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Creates the framework-free application services and connects them to their ports. */
@Configuration(proxyBeanMethods = false)
public class CoreServicesConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SignInService signInService(
            final UserRepository users,
            final IdentityProvidersPort providers,
            final UnitOfWork unitOfWork,
            final Clock clock) {
        return new SignInService(users, providers, unitOfWork, clock);
    }

    @Bean
    SigningKeyService signingKeyService(
            final SigningKeyRepository keys, final UnitOfWork unitOfWork) {
        return new SigningKeyService(keys, unitOfWork);
    }

    @Bean
    TreeService treeService(
            final TreeSettingsRepository trees,
            final FeatureOverridesPort featureOverrides,
            final UnitOfWork unitOfWork) {
        return new TreeService(trees, featureOverrides, unitOfWork);
    }
}
