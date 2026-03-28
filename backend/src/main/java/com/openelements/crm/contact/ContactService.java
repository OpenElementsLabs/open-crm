package com.openelements.crm.contact;

import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Creates a new ContactService.
     *
     * @param contactRepository the contact repository
     * @param companyRepository the company repository
     * @param commentRepository the comment repository
     */
    public ContactService(final ContactRepository contactRepository,
                          final CompanyRepository companyRepository,
                          final CommentRepository commentRepository) {
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
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
                request.birthday());
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
     * Updates an existing contact. Brevo-managed fields (syncedToBrevo, doubleOptIn) are not modified.
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
        applyFields(entity, request.firstName(), request.lastName(), request.email(),
                request.position(), request.gender(), request.linkedInUrl(),
                request.phoneNumber(), request.companyId(), request.language(),
                request.birthday());
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
     * @param firstName partial first name filter (case-insensitive)
     * @param lastName  partial last name filter (case-insensitive)
     * @param email     partial email filter (case-insensitive)
     * @param companyId exact company ID filter
     * @param language  exact language filter
     * @param pageable  pagination and sorting parameters
     * @return a page of contact responses
     */
    @Transactional(readOnly = true)
    public Page<ContactDto> list(final String firstName,
                                      final String lastName,
                                      final String email,
                                      final UUID companyId,
                                      final Language language,
                                      final Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
        Specification<ContactEntity> spec = Specification.where(null);

        if (firstName != null && !firstName.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%"));
        }
        if (lastName != null && !lastName.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%"));
        }
        if (email != null && !email.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }
        if (companyId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("company").get("id"), companyId));
        }
        if (language != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("language"), language));
        }

        return contactRepository.findAll(spec, pageable).map(this::toDto);
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
                              final Language language, final java.time.LocalDate birthday) {
        entity.setFirstName(firstName);
        entity.setLastName(lastName);
        entity.setEmail(email);
        entity.setPosition(position);
        entity.setGender(gender);
        entity.setLinkedInUrl(linkedInUrl);
        entity.setPhoneNumber(phoneNumber);
        entity.setLanguage(language);
        entity.setBirthday(birthday);

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
