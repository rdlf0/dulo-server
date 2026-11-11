package com.dulo.adapter.persistence.jpa;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tunes the connection pool when the JDBC URL points to SQLite: creates the database directory,
 * uses a single connection (SQLite allows one writer at a time) and enables WAL mode and foreign
 * keys.
 */
class SqliteDataSourceCustomizer implements BeanPostProcessor {

    static final String SQLITE_URL_PREFIX = "jdbc:sqlite:";

    private static void createParentDirectory(final String jdbcUrl) {
        String location = jdbcUrl.substring(SQLITE_URL_PREFIX.length());
        final int query = location.indexOf('?');
        if (query >= 0) {
            location = location.substring(0, query);
        }
        if (location.isBlank() || location.startsWith(":memory:") || location.startsWith("file:")) {
            return;
        }
        final Path parent = Path.of(location).toAbsolutePath().getParent();
        try {
            Files.createDirectories(parent);
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot create SQLite directory " + parent, e);
        }
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        if (bean instanceof final HikariDataSource hikari
                && hikari.getJdbcUrl() != null
                && hikari.getJdbcUrl().startsWith(SQLITE_URL_PREFIX)) {
            createParentDirectory(hikari.getJdbcUrl());
            hikari.setMaximumPoolSize(1);
            hikari.addDataSourceProperty("foreign_keys", "true");
            hikari.addDataSourceProperty("journal_mode", "WAL");
            hikari.addDataSourceProperty("busy_timeout", "5000");
        }
        return bean;
    }
}
