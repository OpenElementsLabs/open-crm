package com.openelements.crm.task;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.tag.TagService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final TagService tagService;

    public TaskService(final TaskRepository taskRepository,
                       final CompanyRepository companyRepository,
                       final ContactRepository contactRepository,
                       final TagService tagService) {
        this.taskRepository = taskRepository;
        this.companyRepository = companyRepository;
        this.contactRepository = contactRepository;
        this.tagService = tagService;
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
            entity.setTags(tagService.resolveTagIds(request.tagIds()));
        }

        final TaskEntity saved = taskRepository.saveAndFlush(entity);
        return TaskDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public TaskDto getById(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final TaskEntity entity = findOrThrow(id);
        return TaskDto.fromEntity(entity);
    }

    public TaskDto update(final UUID id, final TaskUpdateDto request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final TaskEntity entity = findOrThrow(id);
        entity.setAction(request.action());
        entity.setDueDate(request.dueDate());
        entity.setStatus(request.status());
        if (request.tagIds() != null) {
            entity.setTags(tagService.resolveTagIds(request.tagIds()));
        }
        final TaskEntity saved = taskRepository.saveAndFlush(entity);
        return TaskDto.fromEntity(saved);
    }

    public void delete(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final TaskEntity entity = findOrThrow(id);
        taskRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public Page<TaskDto> list(final TaskStatus status, final List<UUID> tagIds, final Pageable pageable) {
        final Specification<TaskEntity> spec = buildSpec(status, tagIds);
        return taskRepository.findAll(spec, pageable).map(TaskDto::fromEntity);
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

    private TaskEntity findOrThrow(final UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Task not found: " + id));
    }
}
