package com.dulo.core.tree.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dulo.core.shared.port.out.UnitOfWork;
import com.dulo.core.tree.port.in.TreeView;
import com.dulo.core.tree.port.out.TreeSettingsRepository;
import com.dulo.domain.feature.Feature;
import com.dulo.domain.feature.FeatureOverrides;
import com.dulo.domain.feature.FeatureState;
import com.dulo.domain.tree.TreeSettings;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

class TreeServiceTest {

    private static final UnitOfWork DIRECT =
            new UnitOfWork() {
                @Override
                public <T> T read(final Supplier<T> work) {
                    return work.get();
                }

                @Override
                public <T> T write(final Supplier<T> work) {
                    return work.get();
                }
            };

    @Test
    void emptyBeforeSetup() {
        final TreeService service =
                new TreeService(new InMemoryTrees(null), FeatureOverrides::none, DIRECT);

        assertThat(service.getTree()).isEmpty();
    }

    @Test
    void appliesConfigurationOverrides() {
        final TreeSettings tree = TreeSettings.create("Smiths", null, Set.of(Feature.MAP_VIEW));
        final TreeService service =
                new TreeService(
                        new InMemoryTrees(tree),
                        () -> new FeatureOverrides(Map.of(Feature.MAP_VIEW, false)),
                        DIRECT);

        final TreeView view = service.getTree().orElseThrow();

        assertThat(view.tree()).isEqualTo(tree);
        assertThat(view.features()).contains(new FeatureState(Feature.MAP_VIEW, false, true));
    }

    private record InMemoryTrees(@Nullable TreeSettings tree) implements TreeSettingsRepository {

        @Override
        public Optional<TreeSettings> find() {
            return Optional.ofNullable(this.tree);
        }

        @Override
        public TreeSettings save(final TreeSettings tree) {
            throw new UnsupportedOperationException();
        }
    }
}
