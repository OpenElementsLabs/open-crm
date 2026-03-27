package com.openelements.crm.company;

import com.openelements.crm.comment.CommentCreateRequest;
import com.openelements.crm.comment.CommentResponse;
import com.openelements.crm.comment.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

/**
 * REST controller for company management.
 */
@RestController
@RequestMapping("/api/companies")
@Tag(name = "Companies", description = "Company management operations")
public class CompanyController {

    private final CompanyService companyService;
    private final CommentService commentService;

    /**
     * Creates a new CompanyController.
     *
     * @param companyService the company service
     * @param commentService the comment service
     */
    public CompanyController(final CompanyService companyService, final CommentService commentService) {
        this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
        this.commentService = Objects.requireNonNull(commentService, "commentService must not be null");
    }

    /**
     * Lists companies with pagination, filtering, and sorting.
     *
     * @param name           partial name filter (case-insensitive)
     * @param city           city filter
     * @param country        country filter
     * @param includeDeleted whether to include soft-deleted companies
     * @param pageable       pagination and sorting parameters
     * @return a page of company responses
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List companies", description = "Returns a paginated list of companies with optional filtering")
    public Page<CompanyResponse> list(
            @RequestParam(required = false) final String name,
            @RequestParam(required = false) final String city,
            @RequestParam(required = false) final String country,
            @RequestParam(defaultValue = "false") final boolean includeDeleted,
            @PageableDefault(size = 20, sort = "name") final Pageable pageable) {
        return companyService.list(name, city, country, includeDeleted, pageable);
    }

    /**
     * Returns a company by its ID.
     *
     * @param id the company ID
     * @return the company response
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get company by ID")
    @ApiResponse(responseCode = "200", description = "Company found")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public CompanyResponse getById(@PathVariable final UUID id) {
        return companyService.getById(id);
    }

    /**
     * Creates a new company.
     *
     * @param request the create request
     * @return the created company response
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new company")
    @ApiResponse(responseCode = "201", description = "Company created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public CompanyResponse create(@Valid @RequestBody final CompanyCreateRequest request) {
        return companyService.create(request);
    }

    /**
     * Updates an existing company.
     *
     * @param id      the company ID
     * @param request the update request
     * @return the updated company response
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a company")
    @ApiResponse(responseCode = "200", description = "Company updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public CompanyResponse update(@PathVariable final UUID id,
                                  @Valid @RequestBody final CompanyUpdateRequest request) {
        return companyService.update(id, request);
    }

    /**
     * Soft-deletes a company. Fails if the company still has associated contacts.
     *
     * @param id the company ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a company")
    @ApiResponse(responseCode = "204", description = "Company soft-deleted")
    @ApiResponse(responseCode = "404", description = "Company not found")
    @ApiResponse(responseCode = "409", description = "Company has associated contacts")
    public void delete(@PathVariable final UUID id) {
        companyService.delete(id);
    }

    /**
     * Restores a soft-deleted company.
     *
     * @param id the company ID
     * @return the restored company response
     */
    @PostMapping(value = "/{id}/restore", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Restore a soft-deleted company")
    @ApiResponse(responseCode = "200", description = "Company restored")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public CompanyResponse restore(@PathVariable final UUID id) {
        return companyService.restore(id);
    }

    /**
     * Lists comments for a company, sorted by creation date descending.
     *
     * @param id       the company ID
     * @param pageable pagination parameters
     * @return a page of comment responses
     */
    @GetMapping(value = "/{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List comments for a company")
    @ApiResponse(responseCode = "200", description = "Comments found")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public Page<CommentResponse> listComments(
            @PathVariable final UUID id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) final Pageable pageable) {
        return commentService.listByCompany(id, pageable);
    }

    /**
     * Adds a comment to a company.
     *
     * @param id      the company ID
     * @param request the comment create request
     * @return the created comment response
     */
    @PostMapping(value = "/{id}/comments", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a comment to a company")
    @ApiResponse(responseCode = "201", description = "Comment created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public CommentResponse addComment(@PathVariable final UUID id,
                                      @Valid @RequestBody final CommentCreateRequest request) {
        return commentService.addToCompany(id, request);
    }
}
