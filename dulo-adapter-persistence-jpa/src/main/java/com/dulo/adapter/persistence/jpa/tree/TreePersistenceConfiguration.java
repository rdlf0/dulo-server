package com.dulo.adapter.persistence.jpa.tree;

import com.dulo.core.tree.port.out.TreeSettingsRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TreePersistenceConfiguration {

    @Bean
    TreeSettingsRepository treeSettingsRepository(final TreeSettingsJpaRepository repository) {
        return new TreeSettingsPersistenceAdapter(repository);
    }
}
