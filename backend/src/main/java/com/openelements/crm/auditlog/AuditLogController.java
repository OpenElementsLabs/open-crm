package com.openelements.crm.auditlog;

import com.openelements.crm.security.RequiresItAdmin;
import com.openelements.spring.base.services.audit.AuditLogDto;
import com.openelements.spring.base.services.audit.AuditLogEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Log")
@SecurityRequirement(name = "oidc")
@RequiresItAdmin
public class AuditLogController {

    private final CrmAuditLogRepository auditLogRepository;

    public AuditLogController(final CrmAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "List audit log entries",
        description = "Returns a paginated list of audit log entries with optional filtering by entity type and user. "
            + "Sorted by createdAt descending (newest first). Requires the IT-ADMIN role."
    )
    public Page<AuditLogDto> listAuditLogs(
        @Parameter(description = "Filter by entity type (exact match)")
        @RequestParam(required = false) final String entityType,
        @Parameter(description = "Filter by user name (exact match)")
        @RequestParam(required = false) final String user,
        @Parameter(hidden = true)
        @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC) final Pageable pageable) {
        final Page<AuditLogEntity> entities;
        final boolean hasEntityType = entityType != null && !entityType.isBlank();
        final boolean hasUser = user != null && !user.isBlank();
        if (hasEntityType && hasUser) {
            entities = auditLogRepository.findByEntityTypeAndUserName(entityType, user, pageable);
        } else if (hasEntityType) {
            entities = auditLogRepository.findByEntityType(entityType, pageable);
        } else if (hasUser) {
            entities = auditLogRepository.findByUserName(user, pageable);
        } else {
            entities = auditLogRepository.findAll(pageable);
        }
        return entities.map(AuditLogDto::fromEntity);
    }

    @GetMapping(path = "/entity-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "List distinct entity types",
        description = "Returns the distinct entity-type strings present in the audit log, sorted alphabetically. "
            + "Used to populate the entity-type filter dropdown. Requires the IT-ADMIN role."
    )
    public List<String> listEntityTypes() {
        return auditLogRepository.findDistinctEntityTypes();
    }
}
