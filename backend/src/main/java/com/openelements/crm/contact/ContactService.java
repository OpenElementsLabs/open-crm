package com.openelements.crm.contact;

import com.openelements.crm.ImageData;
import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.tag.TagService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service handling contact business logic including CRUD operations and company validation.
 */
@Service
@Transactional
public class ContactService {

    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final CommentRepository commentRepository;
    private final TagService tagService;

    public ContactService(final ContactRepository contactRepository,
                          final CompanyRepository companyRepository,
                          final CommentRepository commentRepository,
                          final TagService tagService) {
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.tagService = Objects.requireNonNull(tagService, "tagService must not be null");
    }

    /**
     * Creates a new contact.
     *
     * @param request the create request
     * @return the created contact response
     */
    public ContactDto create(final ContactCreateDto request) {
        Objects.requireNonNull(request, "request must not be null");
        final ContactEntity entity = new ContactEntity();
        applyFields(entity, request.firstName(), request.lastName(), request.email(),
                request.position(), request.gender(), request.linkedInUrl(),
                request.phoneNumber(), request.companyId(), request.language(),
                request.birthday(), request.description());
        if (request.tagIds() != null) {
            entity.setTags(tagService.resolveTagIds(request.tagIds()));
        }
        final ContactEntity saved = contactRepository.saveAndFlush(entity);
        return ContactDto.fromEntity(saved, 0);
    }

    /**
     * Returns a contact by its ID.
     *
     * @param id the contact ID
     * @return the contact response
     * @throws ResponseStatusException with 404 if not found
     */
    @Transactional(readOnly = true)
    public ContactDto getById(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final ContactEntity entity = findOrThrow(id);
        return toDto(entity);
    }

    /**
     * Updates an existing contact. Brevo-managed fields (brevoId) are not modified.
     *
     * @param id      the contact ID
     * @param request the update request
     * @return the updated contact response
     * @throws ResponseStatusException with 404 if not found
     */
    public ContactDto update(final UUID id, final ContactUpdateDto request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final ContactEntity entity = findOrThrow(id);
        if (entity.getBrevoId() != null) {
            final List<String> violations = new ArrayList<>();
            if (!Objects.equals(request.firstName(), entity.getFirstName())) {
                violations.add("firstName");
            }
            if (!Objects.equals(request.lastName(), entity.getLastName())) {
                violations.add("lastName");
            }
            if (!Objects.equals(request.email(), entity.getEmail())) {
                violations.add("email");
            }
            if (request.language() != entity.getLanguage()) {
                violations.add("language");
            }
            if (!violations.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The fields " + String.join(", ", violations)
                    + " are managed by Brevo and cannot be modified");
            }
        }
        applyFields(entity, request.firstName(), request.lastName(), request.email(),
                request.position(), request.gender(), request.linkedInUrl(),
                request.phoneNumber(), request.companyId(), request.language(),
                request.birthday(), request.description());
        if (request.tagIds() != null) {
            entity.setTags(tagService.resolveTagIds(request.tagIds()));
        }
        final ContactEntity saved = contactRepository.saveAndFlush(entity);
        return toDto(saved);
    }

    /**
     * Hard-deletes a contact and all associated comments (via database cascade).
     *
     * @param id the contact ID
     * @throws ResponseStatusException with 404 if not found
     */
    public void delete(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final ContactEntity entity = findOrThrow(id);
        commentRepository.deleteByContactId(id);
        contactRepository.delete(entity);
    }

    /**
     * Lists contacts with pagination, filtering, and sorting.
     *
     * @param search    multi-word search across firstName, lastName, email, and company name (case-insensitive)
     * @param companyId exact company ID filter
     * @param language  exact language filter
     * @param brevo     filter by Brevo origin (true = only Brevo, false = only non-Brevo, null = all)
     * @param pageable  pagination and sorting parameters
     * @return a page of contact responses
     */
    @Transactional(readOnly = true)
    public Page<ContactDto> list(final String search,
                                      final UUID companyId,
                                      final boolean noCompany,
                                      final String language,
                                      final Boolean brevo,
                                      final List<UUID> tagIds,
                                      final Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (companyId != null && noCompany) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot combine companyId and noCompany filters");
        }
        Specification<ContactEntity> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            final String[] words = search.trim().split("\\s+");
            for (final String word : words) {
                final String pattern = "%" + word.toLowerCase() + "%";
                spec = spec.and((root, query, cb) -> {
                    final Join<ContactEntity, CompanyEntity> companyJoin = root.join("company", JoinType.LEFT);
                    return cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(companyJoin.get("name")), pattern)
                    );
                });
            }
        }
        if (companyId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("company").get("id"), companyId));
        }
        if (noCompany) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("company")));
        }
        if (language != null && !language.isBlank()) {
            if ("UNKNOWN".equalsIgnoreCase(language)) {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("language")));
            } else {
                final Language lang = Language.valueOf(language.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("language"), lang));
            }
        }
        if (brevo != null) {
            if (brevo) {
                spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("brevoId")));
            } else {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("brevoId")));
            }
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            for (final UUID tagId : tagIds) {
                spec = spec.and((root, query, cb) -> {
                    final var tagsJoin = root.join("tags");
                    return cb.equal(tagsJoin.get("id"), tagId);
                });
            }
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.conjunction();
            });
        }

        return contactRepository.findAll(spec, pageable).map(this::toDto);
    }

    /**
     * Lists all contacts matching the given filters without pagination.
     *
     * @param search    multi-word search across firstName, lastName, email, and company name
     * @param companyId exact company ID filter
     * @param language  exact language filter
     * @param brevo     filter by Brevo origin
     * @return list of all matching contact DTOs
     */
    @Transactional(readOnly = true)
    public List<ContactDto> listAll(final String search,
                                    final UUID companyId,
                                    final boolean noCompany,
                                    final String language,
                                    final Boolean brevo,
                                    final List<UUID> tagIds) {
        if (companyId != null && noCompany) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot combine companyId and noCompany filters");
        }
        Specification<ContactEntity> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            final String[] words = search.trim().split("\\s+");
            for (final String word : words) {
                final String pattern = "%" + word.toLowerCase() + "%";
                spec = spec.and((root, query, cb) -> {
                    final Join<ContactEntity, CompanyEntity> companyJoin = root.join("company", JoinType.LEFT);
                    return cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(companyJoin.get("name")), pattern)
                    );
                });
            }
        }
        if (companyId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("company").get("id"), companyId));
        }
        if (noCompany) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("company")));
        }
        if (language != null && !language.isBlank()) {
            if ("UNKNOWN".equalsIgnoreCase(language)) {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("language")));
            } else {
                final Language lang = Language.valueOf(language.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("language"), lang));
            }
        }
        if (brevo != null) {
            if (brevo) {
                spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("brevoId")));
            } else {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("brevoId")));
            }
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            for (final UUID tagId : tagIds) {
                spec = spec.and((root, query, cb) -> {
                    final var tagsJoin = root.join("tags");
                    return cb.equal(tagsJoin.get("id"), tagId);
                });
            }
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.conjunction();
            });
        }

        return contactRepository.findAll(spec, Sort.by("lastName", "firstName")).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Uploads or replaces the photo for a contact.
     *
     * @param id          the contact ID
     * @param data        the image bytes
     * @param contentType the MIME content type
     * @throws ResponseStatusException with 404 if not found, 400 if content type is not JPEG
     */
    public void uploadPhoto(final UUID id, final byte[] data, final String contentType) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        if (!"image/jpeg".equals(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid content type: " + contentType + ". Only image/jpeg is allowed.");
        }
        final ContactEntity entity = findOrThrow(id);
        entity.setPhoto(data);
        entity.setPhotoContentType(contentType);
        contactRepository.saveAndFlush(entity);
    }

    /**
     * Returns the photo data for a contact.
     *
     * @param id the contact ID
     * @return the image data
     * @throws ResponseStatusException with 404 if not found or no photo exists
     */
    @Transactional(readOnly = true)
    public ImageData getPhoto(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final ContactEntity entity = findOrThrow(id);
        if (entity.getPhoto() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No photo for contact: " + id);
        }
        return new ImageData(entity.getPhoto(), entity.getPhotoContentType());
    }

    /**
     * Removes the photo from a contact.
     *
     * @param id the contact ID
     * @throws ResponseStatusException with 404 if not found
     */
    public void deletePhoto(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final ContactEntity entity = findOrThrow(id);
        entity.setPhoto(null);
        entity.setPhotoContentType(null);
        contactRepository.saveAndFlush(entity);
    }

    private ContactDto toDto(final ContactEntity entity) {
        final long commentCount = commentRepository.countByContactId(entity.getId());
        return ContactDto.fromEntity(entity, commentCount);
    }

    private void applyFields(final ContactEntity entity,
                              final String firstName, final String lastName,
                              final String email, final String position,
                              final Gender gender, final String linkedInUrl,
                              final String phoneNumber, final UUID companyId,
                              final Language language, final java.time.LocalDate birthday,
                              final String description) {
        entity.setFirstName(firstName);
        entity.setLastName(lastName);
        entity.setEmail(email);
        entity.setPosition(position);
        entity.setGender(gender);
        entity.setLinkedInUrl(linkedInUrl);
        entity.setPhoneNumber(phoneNumber);
        entity.setLanguage(language);
        entity.setBirthday(birthday);
        entity.setDescription(description);

        if (companyId != null) {
            final CompanyEntity company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Company not found: " + companyId));
            if (company.isDeleted()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot reference a soft-deleted company: " + companyId);
            }
            entity.setCompany(company);
        } else {
            entity.setCompany(null);
        }
    }

    private ContactEntity findOrThrow(final UUID id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contact not found: " + id));
    }
}
