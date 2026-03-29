package com.openelements.crm.contact;

import com.openelements.crm.ImageData;
import com.openelements.crm.comment.CommentCreateDto;
import com.openelements.crm.comment.CommentDto;
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
 * REST controller for contact management.
 */
@RestController
@RequestMapping("/api/contacts")
@Tag(name = "Contacts", description = "Contact management operations")
public class ContactController {

    private final ContactService contactService;
    private final CommentService commentService;

    /**
     * Creates a new ContactController.
     *
     * @param contactService the contact service
     * @param commentService the comment service
     */
    public ContactController(final ContactService contactService, final CommentService commentService) {
        this.contactService = Objects.requireNonNull(contactService, "contactService must not be null");
        this.commentService = Objects.requireNonNull(commentService, "commentService must not be null");
    }

    /**
     * Lists contacts with pagination, filtering, and sorting.
     *
     * @param firstName partial first name filter
     * @param lastName  partial last name filter
     * @param email     partial email filter
     * @param companyId exact company ID filter
     * @param language  exact language filter
     * @param brevo     filter by Brevo origin (true = only Brevo, false = only non-Brevo, null = all)
     * @param pageable  pagination and sorting parameters
     * @return a page of contact responses
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List contacts", description = "Returns a paginated list of contacts with optional filtering")
    public Page<ContactDto> list(
            @RequestParam(required = false) final String firstName,
            @RequestParam(required = false) final String lastName,
            @RequestParam(required = false) final String email,
            @RequestParam(required = false) final UUID companyId,
            @RequestParam(required = false) final String language,
            @RequestParam(required = false) final Boolean brevo,
            @PageableDefault(size = 20, sort = "lastName") final Pageable pageable) {
        return contactService.list(firstName, lastName, email, companyId, language, brevo, pageable);
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
    public ContactDto getById(@PathVariable final UUID id) {
        return contactService.getById(id);
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
        return contactService.create(request);
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
    public ContactDto update(@PathVariable final UUID id,
                                  @Valid @RequestBody final ContactUpdateDto request) {
        return contactService.update(id, request);
    }

    /**
     * Hard-deletes a contact and all associated comments.
     *
     * @param id the contact ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a contact", description = "Permanently deletes the contact and all associated comments")
    @ApiResponse(responseCode = "204", description = "Contact deleted")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public void delete(@PathVariable final UUID id) {
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
    public void uploadPhoto(@PathVariable final UUID id,
                            @RequestParam("file") final MultipartFile file) {
        try {
            contactService.uploadPhoto(id, file.getBytes(), file.getContentType());
        } catch (final java.io.IOException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Failed to read file");
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
    public ResponseEntity<byte[]> getPhoto(@PathVariable final UUID id) {
        final ImageData imageData = contactService.getPhoto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imageData.contentType()))
                .body(imageData.data());
    }

    /**
     * Removes the photo from a contact.
     *
     * @param id the contact ID
     */
    @DeleteMapping("/{id}/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove contact photo")
    @ApiResponse(responseCode = "204", description = "Photo removed")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public void deletePhoto(@PathVariable final UUID id) {
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
            @PathVariable final UUID id,
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
    public CommentDto addComment(@PathVariable final UUID id,
                                      @Valid @RequestBody final CommentCreateDto request) {
        return commentService.addToContact(id, request);
    }
}
