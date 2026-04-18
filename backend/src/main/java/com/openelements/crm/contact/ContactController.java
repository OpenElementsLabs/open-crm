package com.openelements.crm.contact;

import com.openelements.crm.comment.CommentCreateDto;
import com.openelements.crm.comment.CommentDto;
import com.openelements.crm.comment.CommentService;
import com.openelements.spring.base.security.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * REST controller for contact management.
 */
@RestController
@RequestMapping("/api/contacts")
@Tag(name = "Contacts", description = "Contact management operations")
@SecurityRequirement(name = "oidc")
public class ContactController {

    private final ContactService contactService;
    private final CommentService commentService;
    private final UserService userService;

    /**
     * Creates a new ContactController.
     *
     * @param contactService the contact service
     * @param commentService the comment service
     */
    public ContactController(final ContactService contactService, final CommentService commentService, UserService userService) {
        this.contactService = Objects.requireNonNull(contactService, "contactService must not be null");
        this.commentService = Objects.requireNonNull(commentService, "commentService must not be null");
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
    }

    /**
     * Lists contacts with pagination, filtering, and sorting.
     *
     * @param search    multi-word search across firstName, lastName, email, and company name
     * @param companyId exact company ID filter
     * @param language  exact language filter
     * @param brevo     filter by Brevo origin (true = only Brevo, false = only non-Brevo, null = all)
     * @param pageable  pagination and sorting parameters
     * @return a page of contact responses
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List contacts", description = "Returns a paginated list of contacts with optional filtering")
    public Page<ContactDto> list(
        @Parameter(description = "Multi-word search across name, email, and company (case-insensitive contains)")
        @RequestParam(required = false) final String search,
        @Parameter(description = "Filter by company ID (exact match)")
        @RequestParam(required = false) final UUID companyId,
        @Parameter(description = "Filter by language code (DE, EN, or UNKNOWN for null)")
        @RequestParam(required = false) final String language,
        @Parameter(description = "Filter for contacts without a company association")
        @RequestParam(defaultValue = "false") final boolean noCompany,
        @Parameter(description = "Filter by Brevo origin: true = only Brevo, false = only non-Brevo, omit = all")
        @RequestParam(required = false) final Boolean brevo,
        @Parameter(description = "Filter by tag IDs (AND semantics)")
        @RequestParam(required = false) final List<UUID> tagIds,
        @Parameter(hidden = true)
        @PageableDefault(size = 20, sort = "lastName") final Pageable pageable) {
        return contactService.list(search, companyId, noCompany, language, brevo, tagIds, pageable);
    }

    /**
     * Exports contacts as CSV with selected columns.
     *
     * @param search    multi-word search filter
     * @param companyId exact company ID filter
     * @param language  exact language filter
     * @param brevo     filter by Brevo origin
     * @param columns   list of columns to include
     * @param response  the HTTP response to write CSV to
     */
    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export contacts as CSV")
    @ApiResponse(responseCode = "200", description = "CSV file downloaded")
    public void exportCsv(
        @Parameter(description = "Multi-word search filter") @RequestParam(required = false) final String search,
        @Parameter(description = "Filter by company ID") @RequestParam(required = false) final UUID companyId,
        @Parameter(description = "Filter by language code") @RequestParam(required = false) final String language,
        @Parameter(description = "Filter for contacts without a company") @RequestParam(defaultValue = "false") final boolean noCompany,
        @Parameter(description = "Filter by Brevo origin") @RequestParam(required = false) final Boolean brevo,
        @Parameter(description = "Filter by tag IDs") @RequestParam(required = false) final List<UUID> tagIds,
        @Parameter(description = "Columns to include in the CSV") @RequestParam final List<ContactExportColumn> columns,
        final HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"contacts.csv\"");

        final List<ContactDto> contacts = contactService.listAll(search, companyId, noCompany, language, brevo, tagIds);
        final String[] headers = columns.stream().map(ContactExportColumn::getHeader).toArray(String[]::new);

        final var writer = response.getWriter();
        writer.write('\uFEFF'); // UTF-8 BOM
        try (final CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers).build())) {
            for (final ContactDto contact : contacts) {
                final Object[] values = columns.stream().map(col -> col.extract(contact)).toArray();
                printer.printRecord(values);
            }
        }
    }

    /**
     * Returns a contact by its ID.
     *
     * @param id the contact ID
     * @return the contact response
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get contact by ID")
    @ApiResponse(responseCode = "200", description = "Contact found")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public ContactDto getById(@Parameter(description = "The contact ID") @PathVariable final UUID id) {
        return contactService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * Creates a new contact.
     *
     * @param request the create request
     * @return the created contact response
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new contact")
    @ApiResponse(responseCode = "201", description = "Contact created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ContactDto create(@Valid @RequestBody final ContactCreateDto request) {
        final ContactDto dto = new ContactDto(
            null,
            request.title(),
            request.firstName(),
            request.lastName(),
            request.email(),
            request.position(),
            request.gender(),
            request.socialLinks().stream().map(SocialLinkDto::fromCreateDto).toList(),
            request.phoneNumber(),
            request.description(),
            request.companyId(),
            null,
            0,
            false,
            request.birthday(),
            false,
            false,
            request.language(),
            request.tagIds(),
            null,
            null
        );
        return contactService.save(dto);
    }

    /**
     * Updates an existing contact.
     *
     * @param id      the contact ID
     * @param request the update request
     * @return the updated contact response
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a contact")
    @ApiResponse(responseCode = "200", description = "Contact updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public ContactDto update(@Parameter(description = "The contact ID") @PathVariable final UUID id,
                             @Valid @RequestBody final ContactUpdateDto request) {
        final ContactDto dto = new ContactDto(
            id,
            request.title(),
            request.firstName(),
            request.lastName(),
            request.email(),
            request.position(),
            request.gender(),
            request.socialLinks().stream().map(SocialLinkDto::fromCreateDto).toList(),
            request.phoneNumber(),
            request.description(),
            request.companyId(),
            null,
            0,
            false,
            request.birthday(),
            false,
            false,
            request.language(),
            request.tagIds(),
            null,
            null
        );
        return contactService.save(dto);
    }

    /**
     * Hard-deletes a contact and all associated comments.
     *
     * @param id the contact ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a contact", description = "Permanently deletes the contact and all associated comments")
    @ApiResponse(responseCode = "204", description = "Contact deleted")
    @ApiResponse(responseCode = "403", description = "Missing ADMIN role")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public void delete(@Parameter(description = "The contact ID") @PathVariable final UUID id) {
        contactService.delete(id);
    }

    /**
     * Uploads or replaces the photo for a contact.
     *
     * @param id   the contact ID
     * @param file the image file (JPEG only)
     */
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload contact photo")
    @ApiResponse(responseCode = "200", description = "Photo uploaded")
    @ApiResponse(responseCode = "400", description = "Invalid file format or size")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public void uploadPhoto(@Parameter(description = "The contact ID") @PathVariable final UUID id,
                            @Parameter(description = "The photo image file (JPEG only; max 2 MB)") @RequestParam("file") final MultipartFile file) {
        try {
            contactService.uploadPhoto(id, file.getBytes(), file.getContentType());
        } catch (final java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read file");
        }
    }

    /**
     * Returns the photo for a contact as binary data.
     *
     * @param id the contact ID
     * @return the photo binary data with correct content type
     */
    @GetMapping(value = "/{id}/photo")
    @Operation(summary = "Get contact photo")
    @ApiResponse(responseCode = "200", description = "Photo found")
    @ApiResponse(responseCode = "404", description = "Contact or photo not found")
    public ResponseEntity<byte[]> getPhoto(@Parameter(description = "The contact ID") @PathVariable final UUID id) {
        return contactService.getPhoto(id)
            .map(i -> i.toHttpResponse())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * Removes the photo from a contact.
     *
     * @param id the contact ID
     */
    @DeleteMapping("/{id}/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove contact photo")
    @ApiResponse(responseCode = "204", description = "Photo removed")
    @ApiResponse(responseCode = "403", description = "Missing ADMIN role")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public void deletePhoto(@Parameter(description = "The contact ID") @PathVariable final UUID id) {
        contactService.deletePhoto(id);
    }

    /**
     * Lists comments for a contact, sorted by creation date descending.
     *
     * @param id       the contact ID
     * @param pageable pagination parameters
     * @return a page of comment responses
     */
    @GetMapping(value = "/{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List comments for a contact")
    @ApiResponse(responseCode = "200", description = "Comments found")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public Page<CommentDto> listComments(
        @Parameter(description = "The contact ID") @PathVariable final UUID id,
        @Parameter(hidden = true)
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) final Pageable pageable) {
        return commentService.listByContact(id, pageable);
    }

    /**
     * Adds a comment to a contact.
     *
     * @param id      the contact ID
     * @param request the comment create request
     * @return the created comment response
     */
    @PostMapping(value = "/{id}/comments", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a comment to a contact")
    @ApiResponse(responseCode = "201", description = "Comment created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public CommentDto addComment(@Parameter(description = "The contact ID") @PathVariable final UUID id,
                                 @Valid @RequestBody final CommentCreateDto request) {
        final CommentDto commentDto = new CommentDto(null,
            request.text(),
            userService.getCurrentUser().name(),
            null,
            id,
            null,
            null,
            null);
        return commentService.save(commentDto);
    }
}
