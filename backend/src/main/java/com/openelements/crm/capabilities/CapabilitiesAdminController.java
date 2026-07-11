package com.openelements.crm.capabilities;

import com.openelements.crm.contact.CrmHeicSupportCheck;
import com.openelements.spring.base.security.roles.RequiresItAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only REST controller reporting which optional runtime capabilities are available in the
 * running container, so an operator can confirm from the admin status page whether e.g. HEIC
 * decoding is actually installed — rather than having to read a startup log line.
 *
 * <p>The underlying values are probed exactly once at startup and cached by their beans; this
 * endpoint is a thin read that always answers HTTP 200. IT-ADMIN only, consistent with the rest
 * of {@code /api/admin/*}.
 */
@RestController
@RequestMapping("/api/admin/capabilities")
@Tag(name = "Capabilities Admin", description = "Runtime capability flags for the admin status page")
@SecurityRequirement(name = "oidc")
@RequiresItAdmin
public class CapabilitiesAdminController {

    private final CrmHeicSupportCheck heicSupportCheck;

    /**
     * Creates a new {@code CapabilitiesAdminController}.
     *
     * @param heicSupportCheck the startup HEIC-support probe bean
     */
    public CapabilitiesAdminController(final CrmHeicSupportCheck heicSupportCheck) {
        this.heicSupportCheck =
            Objects.requireNonNull(heicSupportCheck, "heicSupportCheck must not be null");
    }

    /**
     * Reports the runtime capabilities of the container. Reflects the startup-cached values;
     * always HTTP 200.
     *
     * @return the capabilities DTO
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get runtime capability flags")
    @ApiResponse(responseCode = "200", description = "Capabilities returned")
    @ApiResponse(responseCode = "403", description = "Caller lacks the IT-ADMIN role")
    public CapabilitiesDto getCapabilities() {
        return new CapabilitiesDto(heicSupportCheck.isHeicAvailable());
    }
}
