package com.openelements.crm.search.lib;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * Shared mutable flag indicating whether {@link MeilisearchBootstrapRunner} has
 * finished the initial reindex. The bootstrap runs asynchronously and search
 * endpoints short-circuit (e.g. to 503) while this is {@code true}.
 */
@Component
public class SearchReadinessState {

    private final AtomicBoolean bootstrapping = new AtomicBoolean(true);

    public boolean isBootstrapping() {
        return bootstrapping.get();
    }

    public void markBootstrappingStarted() {
        bootstrapping.set(true);
    }

    public void markBootstrappingFinished() {
        bootstrapping.set(false);
    }
}
