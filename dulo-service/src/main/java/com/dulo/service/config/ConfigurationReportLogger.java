package com.dulo.service.config;

import com.dulo.core.tree.port.in.GetTreeUseCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Logs the merged configuration on start while the initial setup is pending, or always when {@code
 * dulo.print-config} is true.
 */
class ConfigurationReportLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationReportLogger.class);

    private final ConfigurationReport report;
    private final DuloProperties properties;
    private final GetTreeUseCase getTree;

    ConfigurationReportLogger(
            final ConfigurationReport report,
            final DuloProperties properties,
            final GetTreeUseCase getTree) {
        this.report = report;
        this.properties = properties;
        this.getTree = getTree;
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        // TODO(M2): use the setup state instead of the tree's existence.
        final boolean setupPending = getTree.getTree().isEmpty();
        if (setupPending || properties.printConfig()) {
            LOG.info(
                    "{}Merged configuration:\n{}",
                    setupPending ? "Initial setup pending. " : "",
                    report.render(properties));
        }
    }
}
