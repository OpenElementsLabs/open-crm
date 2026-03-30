package com.openelements.crm.company;

import com.openelements.crm.ImageData;
import com.openelements.crm.comment.CommentCreateDto;
import com.openelements.crm.comment.CommentDto;
import com.openelements.crm.comment.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for company management.
 */
@RestController
@RequestMapping("/api/companies")
@Tag(name = "Companies", description = "Company management operations")
@SecurityRequirement(name = "oidc")
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
     * @param includeDeleted whether to include soft-deleted companies
     * @param brevo          filter by Brevo origin (true = only Brevo, false = only non-Brevo, null = all)
     * @param pageable       pagination and sorting parameters
     * @return a page of company responses
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List companies", description = "Returns a paginated list of companies with optional filtering")
    public Page<CompanyDto> list(
            @Parameter(description = "Partial company name filter (case-insensitive contains)")
            @RequestParam(required = false) final String name,
            @Parameter(description = "Whether to include soft-deleted companies")
            @RequestParam(defaultValue = "false") final boolean includeDeleted,
            @Parameter(description = "Filter by Brevo origin: true = only Brevo, false = only non-Brevo, omit = all")
            @RequestParam(required = false) final Boolean brevo,
            @Parameter(description = "Filter by tag IDs (AND semantics)")
            @RequestParam(required = false) final List<UUID> tagIds,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "name") final Pageable pageable) {
        return companyService.list(name, includeDeleted, brevo, tagIds, pageable);
    }

    /**
     * Exports companies as CSV with selected columns.
     *
     * @param name           partial name filter
     * @param includeDeleted whether to include soft-deleted companies
     * @param brevo          filter by Brevo origin
     * @param columns        list of columns to include
     * @param response       the HTTP response to write CSV to
     */
    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export companies as CSV")
    @ApiResponse(responseCode = "200", description = "CSV file downloaded")
    public void exportCsv(
            @Parameter(description = "Partial company name filter") @RequestParam(required = false) final String name,
            @Parameter(description = "Whether to include soft-deleted companies") @RequestParam(defaultValue = "false") final boolean includeDeleted,
            @Parameter(description = "Filter by Brevo origin") @RequestParam(required = false) final Boolean brevo,
            @Parameter(description = "Filter by tag IDs") @RequestParam(required = false) final List<UUID> tagIds,
            @Parameter(description = "Columns to include in the CSV") @RequestParam final List<CompanyExportColumn> columns,
            final HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"companies.csv\"");

        final List<CompanyDto> companies = companyService.listAll(name, includeDeleted, brevo, tagIds);
        final String[] headers = columns.stream().map(CompanyExportColumn::getHeader).toArray(String[]::new);

        final var writer = response.getWriter();
        writer.write('\uFEFF'); // UTF-8 BOM
        try (final CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers).build())) {
            for (final CompanyDto company : companies) {
                final Object[] values = columns.stream().map(col -> col.extract(company)).toArray();
                printer.printRecord(values);
            }
        }
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
    public CompanyDto getById(@Parameter(description = "The company ID") @PathVariable final UUID id) {
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
    public CompanyDto create(@Valid @RequestBody final CompanyCreateDto request) {
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
    public CompanyDto update(@Parameter(description = "The company ID") @PathVariable final UUID id,
                                  @Valid @RequestBody final CompanyUpdateDto request) {
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
    public void delete(@Parameter(description = "The company ID") @PathVariable final UUID id) {
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
    public CompanyDto restore(@Parameter(description = "The company ID") @PathVariable final UUID id) {
        return companyService.restore(id);
    }

    /**
     * Uploads or replaces the logo for a company.
     *
     * @param id   the company ID
     * @param file the image file
     */
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload company logo")
    @ApiResponse(responseCode = "200", description = "Logo uploaded")
    @ApiResponse(responseCode = "400", description = "Invalid file format or size")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public void uploadLogo(@Parameter(description = "The company ID") @PathVariable final UUID id,
                           @Parameter(description = "The logo image file (JPEG, PNG, or SVG; max 2 MB)") @RequestParam("file") final MultipartFile file) {
        try {
            companyService.uploadLogo(id, file.getBytes(), file.getContentType());
        } catch (final java.io.IOException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Failed to read file");
        }
    }

    /**
     * Returns the logo for a company as binary data.
     *
     * @param id the company ID
     * @return the logo binary data with correct content type
     */
    @GetMapping(value = "/{id}/logo")
    @Operation(summary = "Get company logo")
    @ApiResponse(responseCode = "200", description = "Logo found")
    @ApiResponse(responseCode = "404", description = "Company or logo not found")
    public ResponseEntity<byte[]> getLogo(@Parameter(description = "The company ID") @PathVariable final UUID id) {
        final ImageData imageData = companyService.getLogo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imageData.contentType()))
                .body(imageData.data());
    }

    /**
     * Removes the logo from a company.
     *
     * @param id the company ID
     */
    @DeleteMapping("/{id}/logo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove company logo")
    @ApiResponse(responseCode = "204", description = "Logo removed")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public void deleteLogo(@Parameter(description = "The company ID") @PathVariable final UUID id) {
        companyService.deleteLogo(id);
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
    public Page<CommentDto> listComments(
            @Parameter(description = "The company ID") @PathVariable final UUID id,
            @Parameter(hidden = true)
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
    public CommentDto addComment(@Parameter(description = "The company ID") @PathVariable final UUID id,
                                      @Valid @RequestBody final CommentCreateDto request) {
        return commentService.addToCompany(id, request);
    }
}
