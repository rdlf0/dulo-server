package com.dulo.adapter.persistence.jpa;

import com.dulo.adapter.persistence.jpa.tree.TreePersistenceConfiguration;
import com.dulo.core.shared.port.out.UnitOfWork;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;

/** Entry point of the JPA persistence adapter. Import it to activate the adapter. */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = PersistenceJpaConfiguration.class)
@EnableJpaRepositories(basePackageClasses = PersistenceJpaConfiguration.class)
@Import(TreePersistenceConfiguration.class)
public class PersistenceJpaConfiguration {

    @Bean
    static SqliteDataSourceCustomizer sqliteDataSourceCustomizer() {
        return new SqliteDataSourceCustomizer();
    }

    @Bean
    UnitOfWork unitOfWork(final PlatformTransactionManager transactionManager) {
        return new TransactionalUnitOfWork(transactionManager);
    }
}
