package com.openelements.crm.task;

import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.services.tag.TagRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TaskService(final TaskRepository taskRepository,
                       final CompanyRepository companyRepository,
                       final ContactRepository contactRepository,
                       final CommentRepository commentRepository,
                       final TagRepository tagRepository,
                       final ApplicationEventPublisher eventPublisher) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.tagRepository = Objects.requireNonNull(tagRepository, "tagRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    public TaskDto create(final TaskCreateDto request) {
        Objects.requireNonNull(request, "request must not be null");

        final boolean hasCompany = request.companyId() != null;
        final boolean hasContact = request.contactId() != null;
        if (hasCompany == hasContact) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Exactly one of companyId or contactId must be provided");
        }

        final TaskEntity entity = new TaskEntity();
        entity.setAction(request.action());
        entity.setDueDate(request.dueDate());
        entity.setStatus(request.status() != null ? request.status() : TaskStatus.OPEN);

        if (hasCompany) {
            final CompanyEntity company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Company not found: " + request.companyId()));
            entity.setCompany(company);
        } else {
            final ContactEntity contact = contactRepository.findById(request.contactId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Contact not found: " + request.contactId()));
            entity.setContact(contact);
        }

        if (request.tagIds() != null) {
            entity.setTags(tagRepository.findAll(request.tagIds()));
        }

        final TaskEntity saved = taskRepository.saveAndFlush(entity);
        final TaskDto dto = toDto(saved);
        eventPublisher.publishEvent(new WebhookEvent(WebhookEventType.TASK_CREATED, saved.getId(), dto));
        return dto;
    }

    @Transactional(readOnly = true)
    public TaskDto getById(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final TaskEntity entity = findOrThrow(id);
        return toDto(entity);
    }

    public TaskDto update(final UUID id, final TaskUpdateDto request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final TaskEntity entity = findOrThrow(id);
        entity.setAction(request.action());
        entity.setDueDate(request.dueDate());
        entity.setStatus(request.status());
        if (request.tagIds() != null) {
            entity.setTags(tagRepository.findAll(request.tagIds()));
        }
        final TaskEntity saved = taskRepository.saveAndFlush(entity);
        final TaskDto dto = toDto(saved);
        eventPublisher.publishEvent(new WebhookEvent(WebhookEventType.TASK_UPDATED, saved.getId(), dto));
        return dto;
    }

    public void delete(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final TaskEntity entity = findOrThrow(id);
        commentRepository.deleteByTaskId(id);
        taskRepository.delete(entity);
        eventPublisher.publishEvent(new WebhookEvent(WebhookEventType.TASK_DELETED, id, null));
    }

    @Transactional(readOnly = true)
    public Page<TaskDto> list(final TaskStatus status, final List<UUID> tagIds, final Pageable pageable) {
        final Specification<TaskEntity> spec = buildSpec(status, tagIds);
        return taskRepository.findAll(spec, pageable).map(this::toDto);
    }

    private Specification<TaskEntity> buildSpec(final TaskStatus status, final List<UUID> tagIds) {
        return (root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (tagIds != null && !tagIds.isEmpty()) {
                final Join<TaskEntity, ?> tagJoin = root.join("tags");
                predicates.add(tagJoin.get("id").in(tagIds));
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TaskDto toDto(final TaskEntity entity) {
        final long commentCount = commentRepository.countByTaskId(entity.getId());
        return TaskDto.fromEntity(entity, commentCount);
    }

    private TaskEntity findOrThrow(final UUID id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Task not found: " + id));
    }
}
