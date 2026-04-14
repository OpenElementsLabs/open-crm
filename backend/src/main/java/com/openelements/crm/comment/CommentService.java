package com.openelements.crm.comment;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.task.TaskEntity;
import com.openelements.crm.task.TaskRepository;
import com.openelements.crm.webhook.WebhookEvent;
import com.openelements.crm.webhook.WebhookEventType;
import java.util.Objects;
import java.util.UUID;

import com.openelements.spring.base.security.user.UserService;
import org.springframework.context.ApplicationEventPublisher;
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
    private final TaskRepository taskRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new CommentService.
     *
     * @param commentRepository the comment repository
     * @param companyRepository the company repository
     * @param contactRepository the contact repository
     * @param taskRepository    the task repository
     * @param userService       the user service
     * @param eventPublisher    the event publisher
     */
    public CommentService(final CommentRepository commentRepository,
                          final CompanyRepository companyRepository,
                          final ContactRepository contactRepository,
                          final TaskRepository taskRepository,
                          final UserService userService,
                          final ApplicationEventPublisher eventPublisher) {
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    /**
     * Adds a comment to a company. Comments on soft-deleted companies are allowed.
     *
     * @param companyId the company ID
     * @param request   the comment create request
     * @return the created comment response
     * @throws ResponseStatusException with 404 if company not found
     */
    public CommentDto addToCompany(final UUID companyId, final CommentCreateDto request) {
        Objects.requireNonNull(companyId, "companyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Company not found: " + companyId));
        final CommentEntity entity = new CommentEntity();
        entity.setText(request.text());
        entity.setAuthor(userService.getCurrentUser().name());
        entity.setCompany(company);
        final CommentEntity saved = commentRepository.saveAndFlush(entity);
        final CommentDto dto = CommentDto.fromEntity(saved);
        eventPublisher.publishEvent(new WebhookEvent(WebhookEventType.COMMENT_CREATED, saved.getId(), dto));
        return dto;
    }

    /**
     * Adds a comment to a contact.
     *
     * @param contactId the contact ID
     * @param request   the comment create request
     * @return the created comment response
     * @throws ResponseStatusException with 404 if contact not found
     */
    public CommentDto addToContact(final UUID contactId, final CommentCreateDto request) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final ContactEntity contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contact not found: " + contactId));
        final CommentEntity entity = new CommentEntity();
        entity.setText(request.text());
        entity.setAuthor(userService.getCurrentUser().name());
        entity.setContact(contact);
        final CommentEntity saved = commentRepository.saveAndFlush(entity);
        final CommentDto dto = CommentDto.fromEntity(saved);
        eventPublisher.publishEvent(new WebhookEvent(WebhookEventType.COMMENT_CREATED, saved.getId(), dto));
        return dto;
    }

    /**
     * Updates an existing comment.
     *
     * @param id      the comment ID
     * @param request the update request
     * @return the updated comment response
     * @throws ResponseStatusException with 404 if not found
     */
    public CommentDto update(final UUID id, final CommentUpdateDto request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final CommentEntity entity = findOrThrow(id);
        entity.setText(request.text());
        final CommentEntity saved = commentRepository.saveAndFlush(entity);
        final CommentDto dto = CommentDto.fromEntity(saved);
        eventPublisher.publishEvent(new WebhookEvent(WebhookEventType.COMMENT_UPDATED, saved.getId(), dto));
        return dto;
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
        eventPublisher.publishEvent(new WebhookEvent(WebhookEventType.COMMENT_DELETED, id, null));
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
    public Page<CommentDto> listByCompany(final UUID companyId, final Pageable pageable) {
        Objects.requireNonNull(companyId, "companyId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found: " + companyId);
        }
        return commentRepository.findByCompanyId(companyId, pageable).map(CommentDto::fromEntity);
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
    public Page<CommentDto> listByContact(final UUID contactId, final Pageable pageable) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (!contactRepository.existsById(contactId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found: " + contactId);
        }
        return commentRepository.findByContactId(contactId, pageable).map(CommentDto::fromEntity);
    }

    /**
     * Adds a comment to a task.
     *
     * @param taskId  the task ID
     * @param request the comment create request
     * @return the created comment response
     * @throws ResponseStatusException with 404 if task not found
     */
    public CommentDto addToTask(final UUID taskId, final CommentCreateDto request) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Task not found: " + taskId));
        final CommentEntity entity = new CommentEntity();
        entity.setText(request.text());
        entity.setAuthor(userService.getCurrentUser().name());
        entity.setTask(task);
        final CommentEntity saved = commentRepository.saveAndFlush(entity);
        final CommentDto dto = CommentDto.fromEntity(saved);
        eventPublisher.publishEvent(new WebhookEvent(WebhookEventType.COMMENT_CREATED, saved.getId(), dto));
        return dto;
    }

    /**
     * Lists comments for a task, paginated.
     *
     * @param taskId   the task ID
     * @param pageable pagination parameters
     * @return a page of comment responses
     * @throws ResponseStatusException with 404 if task not found
     */
    @Transactional(readOnly = true)
    public Page<CommentDto> listByTask(final UUID taskId, final Pageable pageable) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (!taskRepository.existsById(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId);
        }
        return commentRepository.findByTaskId(taskId, pageable).map(CommentDto::fromEntity);
    }

    private CommentEntity findOrThrow(final UUID id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Comment not found: " + id));
    }
}
