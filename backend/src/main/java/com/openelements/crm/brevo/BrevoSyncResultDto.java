package com.openelements.crm.brevo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Brevo import result summary")
record BrevoSyncResultDto(
        int companiesImported,
        int companiesUpdated,
        int companiesFailed,
        int companiesUnlinked,
        int contactsImported,
        int contactsUpdated,
        int contactsFailed,
        int contactsUnlinked,
        List<String> errors) {
}
