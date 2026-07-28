package com.openelements.crm.opportunity;

import com.openelements.spring.base.security.roles.RequiresAppAdmin;
import com.openelements.spring.base.services.comment.CommentCreateDto;
import com.openelements.spring.base.services.comment.CommentDto;
import com.openelements.spring.base.services.user.UserDto;
import com.openelements.spring.base.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * REST controller for opportunity management.
 */
@RestController
@RequestMapping("/api/opportunities")
@Tag(name = "Opportunities", description = "Sales opportunity (deal) management operations")
@SecurityRequirement(name = "oidc")
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final UserService userService;

    public OpportunityController(final OpportunityService opportunityService, final UserService userService) {
        this.opportunityService = Objects.requireNonNull(opportunityService, "opportunityService must not be null");
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List opportunities", description = "Returns a paginated list of opportunities with optional filtering")
    public Page<OpportunityDto> list(
        @Parameter(description = "Case-insensitive title contains filter")
        @RequestParam(required = false) final String search,
        @Parameter(description = "Filter by status")
        @RequestParam(required = false) final OpportunityStatus status,
        @Parameter(description = "Filter by exact stage value")
        @RequestParam(required = false) final String stage,
        @Parameter(description = "Filter by company ID")
        @RequestParam(required = false) final UUID companyId,
        @Parameter(description = "Filter by contact ID (main or additional contact)")
        @RequestParam(required = false) final UUID contactId,
        @Parameter(description = "Filter by owner user ID")
        @RequestParam(required = false) final UUID ownerId,
        @Parameter(description = "Filter by tag IDs (AND semantics)")
        @RequestParam(required = false) final List<UUID> tagIds,
        @Parameter(hidden = true)
        @PageableDefault(size = 20, sort = "updatedAt") final Pageable pageable) {
        return opportunityService.list(search, status, stage, companyId, contactId, ownerId, tagIds, pageable);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get opportunity by ID")
    @ApiResponse(responseCode = "200", description = "Opportunity found")
    @ApiResponse(responseCode = "404", description = "Opportunity not found")
    public OpportunityDto getById(@Parameter(description = "The opportunity ID") @PathVariable final UUID id) {
        return opportunityService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found"));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new opportunity")
    @ApiResponse(responseCode = "201", description = "Opportunity created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public OpportunityDto create(@Valid @RequestBody final OpportunityCreateDto request) {
        final UUID ownerId = request.ownerId() != null
            ? request.ownerId()
            : userService.getCurrentUserEntity().getId();
        final OpportunityStatus status = request.status() != null ? request.status() : OpportunityStatus.OPEN;
        final OpportunityDto dto = new OpportunityDto(null,
            request.title(),
            request.stage(),
            status,
            request.product(),
            request.estimatedValue(),
            request.companyId(),
            null,
            request.mainContactId(),
            null,
            request.additionalContactIds(),
            ownerRef(ownerId),
            request.tagIds(),
            0,
            null,
            null);
        return opportunityService.save(dto);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an opportunity")
    @ApiResponse(responseCode = "200", description = "Opportunity updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Opportunity not found")
    public OpportunityDto update(@Parameter(description = "The opportunity ID") @PathVariable final UUID id,
                                 @Valid @RequestBody final OpportunityUpdateDto request) {
        final OpportunityDto current = opportunityService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found"));
        final OpportunityDto dto = new OpportunityDto(id,
            request.title(),
            request.stage(),
            request.status(),
            request.product(),
            request.estimatedValue(),
            request.companyId(),
            null,
            request.mainContactId(),
            null,
            request.additionalContactIds(),
            ownerRef(request.ownerId()),
            request.tagIds(),
            current.commentCount(),
            current.createdAt(),
            current.updatedAt());
        return opportunityService.save(dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresAppAdmin
    @Operation(summary = "Delete an opportunity")
    @ApiResponse(responseCode = "204", description = "Opportunity deleted")
    @ApiResponse(responseCode = "403", description = "Missing ADMIN role")
    @ApiResponse(responseCode = "404", description = "Opportunity not found")
    public void delete(@Parameter(description = "The opportunity ID") @PathVariable final UUID id) {
        opportunityService.delete(id);
    }

    @GetMapping(value = "/{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List comments for an opportunity")
    @ApiResponse(responseCode = "200", description = "Comments found")
    @ApiResponse(responseCode = "404", description = "Opportunity not found")
    public List<CommentDto> listComments(@Parameter(description = "The opportunity ID") @PathVariable final UUID id) {
        return opportunityService.listCommentsOfOpportunity(id);
    }

    @PostMapping(value = "/{id}/comments", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a comment to an opportunity")
    @ApiResponse(responseCode = "201", description = "Comment created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Opportunity not found")
    public CommentDto addComment(@Parameter(description = "The opportunity ID") @PathVariable final UUID id,
                                 @Valid @RequestBody final CommentCreateDto request) {
        return opportunityService.addCommentToOpportunity(id, request);
    }

    @PutMapping(value = "/{id}/comments/{commentId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a comment of an opportunity")
    @ApiResponse(responseCode = "200", description = "Comment updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Opportunity or comment not found, or mismatched owner")
    public CommentDto updateComment(@Parameter(description = "The opportunity ID") @PathVariable final UUID id,
                                    @Parameter(description = "The comment ID") @PathVariable final UUID commentId,
                                    @Valid @RequestBody final CommentCreateDto request) {
        return opportunityService.updateCommentOfOpportunity(id, commentId, request);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresAppAdmin
    @Operation(summary = "Delete a comment of an opportunity")
    @ApiResponse(responseCode = "204", description = "Comment deleted")
    @ApiResponse(responseCode = "403", description = "Missing ADMIN role")
    @ApiResponse(responseCode = "404", description = "Opportunity or comment not found, or mismatched owner")
    public void deleteComment(@Parameter(description = "The opportunity ID") @PathVariable final UUID id,
                              @Parameter(description = "The comment ID") @PathVariable final UUID commentId) {
        opportunityService.deleteCommentOfOpportunity(id, commentId);
    }

    /**
     * Builds a minimal owner reference carrying only the ID; the service resolves the full user and
     * {@link OpportunityService#toData} re-populates the nested {@link UserDto} on the response.
     */
    private static UserDto ownerRef(final UUID ownerId) {
        return new UserDto(ownerId, null, null, null, null, null);
    }
}
