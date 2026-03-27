package com.openelements.crm.comment;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service handling comment business logic.
 */
@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;

    /**
     * Creates a new CommentService.
     *
     * @param commentRepository the comment repository
     * @param companyRepository the company repository
     * @param contactRepository the contact repository
     */
    public CommentService(final CommentRepository commentRepository,
                          final CompanyRepository companyRepository,
                          final ContactRepository contactRepository) {
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
    }

    /**
     * Adds a comment to a company. Comments on soft-deleted companies are allowed.
     *
     * @param companyId the company ID
     * @param request   the comment create request
     * @return the created comment response
     * @throws ResponseStatusException with 404 if company not found
     */
    public CommentResponse addToCompany(final UUID companyId, final CommentCreateRequest request) {
        Objects.requireNonNull(companyId, "companyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Company not found: " + companyId));
        final CommentEntity entity = new CommentEntity();
        entity.setText(request.text());
        entity.setAuthor(request.author());
        entity.setCompany(company);
        final CommentEntity saved = commentRepository.saveAndFlush(entity);
        return CommentResponse.fromEntity(saved);
    }

    /**
     * Adds a comment to a contact.
     *
     * @param contactId the contact ID
     * @param request   the comment create request
     * @return the created comment response
     * @throws ResponseStatusException with 404 if contact not found
     */
    public CommentResponse addToContact(final UUID contactId, final CommentCreateRequest request) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final ContactEntity contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contact not found: " + contactId));
        final CommentEntity entity = new CommentEntity();
        entity.setText(request.text());
        entity.setAuthor(request.author());
        entity.setContact(contact);
        final CommentEntity saved = commentRepository.saveAndFlush(entity);
        return CommentResponse.fromEntity(saved);
    }

    /**
     * Updates an existing comment.
     *
     * @param id      the comment ID
     * @param request the update request
     * @return the updated comment response
     * @throws ResponseStatusException with 404 if not found
     */
    public CommentResponse update(final UUID id, final CommentUpdateRequest request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final CommentEntity entity = findOrThrow(id);
        entity.setText(request.text());
        entity.setAuthor(request.author());
        final CommentEntity saved = commentRepository.saveAndFlush(entity);
        return CommentResponse.fromEntity(saved);
    }

    /**
     * Deletes a comment.
     *
     * @param id the comment ID
     * @throws ResponseStatusException with 404 if not found
     */
    public void delete(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final CommentEntity entity = findOrThrow(id);
        commentRepository.delete(entity);
    }

    /**
     * Lists comments for a company, paginated.
     *
     * @param companyId the company ID
     * @param pageable  pagination parameters
     * @return a page of comment responses
     * @throws ResponseStatusException with 404 if company not found
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> listByCompany(final UUID companyId, final Pageable pageable) {
        Objects.requireNonNull(companyId, "companyId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found: " + companyId);
        }
        return commentRepository.findByCompanyId(companyId, pageable).map(CommentResponse::fromEntity);
    }

    /**
     * Lists comments for a contact, paginated.
     *
     * @param contactId the contact ID
     * @param pageable  pagination parameters
     * @return a page of comment responses
     * @throws ResponseStatusException with 404 if contact not found
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> listByContact(final UUID contactId, final Pageable pageable) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (!contactRepository.existsById(contactId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found: " + contactId);
        }
        return commentRepository.findByContactId(contactId, pageable).map(CommentResponse::fromEntity);
    }

    private CommentEntity findOrThrow(final UUID id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Comment not found: " + id));
    }
}
