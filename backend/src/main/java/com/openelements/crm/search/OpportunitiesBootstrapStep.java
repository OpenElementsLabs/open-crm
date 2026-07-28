package com.openelements.crm.search;

import com.openelements.crm.opportunity.OpportunityService;
import com.openelements.spring.base.services.search.SearchIndexBootstrapStep;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstrap step that streams all opportunities into the opportunities index. Reuses
 * {@link OpportunityService#findAllForIndex()} for the entity-to-DTO mapping and
 * {@link SearchIndexService#opportunityDocument} for the DTO-to-document mapping.
 */
@Component
@Order(50)
public class OpportunitiesBootstrapStep implements SearchIndexBootstrapStep {

    private final CrmIndexNames indexNames;
    private final OpportunityService opportunityService;
    private final SearchIndexService indexService;

    public OpportunitiesBootstrapStep(final CrmIndexNames indexNames,
                                      final OpportunityService opportunityService,
                                      final SearchIndexService indexService) {
        this.indexNames = Objects.requireNonNull(indexNames);
        this.opportunityService = Objects.requireNonNull(opportunityService);
        this.indexService = Objects.requireNonNull(indexService);
    }

    @Override
    public String indexUid() {
        return indexNames.opportunities();
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<Map<String, Object>> documents() {
        return opportunityService.findAllForIndex().stream()
            .map(indexService::opportunityDocument)
            .toList()
            .stream();
    }
}
