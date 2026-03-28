package com.openelements.crm.brevo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Response DTO summarizing the result of a Brevo import synchronization.
 *
 * @param companiesImported number of new companies created
 * @param companiesUpdated  number of existing companies updated
 * @param companiesFailed   number of companies that failed to import
 * @param contactsImported  number of new contacts created
 * @param contactsUpdated   number of existing contacts updated
 * @param contactsFailed    number of contacts that failed to import
 * @param errors            list of error messages encountered during sync
 */
@Schema(description = "Brevo import result summary")
record BrevoSyncResultDto(
        int companiesImported,
        int companiesUpdated,
        int companiesFailed,
        int contactsImported,
        int contactsUpdated,
        int contactsFailed,
        List<String> errors) {
}
