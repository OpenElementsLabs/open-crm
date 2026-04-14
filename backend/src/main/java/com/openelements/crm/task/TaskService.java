package com.openelements.crm.task;

import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.data.AbstractDbBackedDataService;
import com.openelements.spring.base.data.EntityRepository;
import com.openelements.spring.base.services.tag.TagEntity;
import com.openelements.spring.base.services.tag.TagRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class TaskService extends AbstractDbBackedDataService<TaskEntity, TaskDto> {

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
        super(eventPublisher);
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.tagRepository = Objects.requireNonNull(tagRepository, "tagRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
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

    @Override
    protected TaskEntity createDetachedEntity() {
        return new TaskEntity();
    }

    @Override
    protected void updateEntity(TaskEntity entity, TaskDto data) {
        entity.setAction(data.action());
        entity.setDueDate(data.dueDate());
        entity.setStatus(data.status());
        if (data.companyId() != null) {
            companyRepository.findById(data.companyId()).ifPresent(entity::setCompany);
        } else {
            entity.setCompany(null);
        }
        if (data.contactId() != null) {
            contactRepository.findById(data.contactId()).ifPresent(entity::setContact);
        } else {
            entity.setContact(null);
        }
        if (data.tagIds() != null) {
            final List<TagEntity> tags = tagRepository.findAllById(data.tagIds());
            entity.setTags(Set.copyOf(tags));
        } else {
            entity.setTags(Set.of());
        }
    }

    @Override
    protected TaskDto toData(TaskEntity entity) {
        return new TaskDto(
            entity.getId(),
            entity.getAction(),
            entity.getDueDate(),
            entity.getStatus(),
            entity.getCompany() != null ? entity.getCompany().getId() : null,
            entity.getCompany() != null ? entity.getCompany().getName() : null,
            entity.getContact() != null ? entity.getContact().getId() : null,
            entity.getContact() != null ? (entity.getContact().getFirstName() + " " + entity.getContact().getLastName()) : null,
            entity.getTags().stream().map(TagEntity::getId).toList(),
            commentRepository.countByTaskId(entity.getId()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    @Override
    protected EntityRepository<TaskEntity> getRepository() {
        return taskRepository;
    }

    public long countWithTag(UUID tagId) {
        Objects.requireNonNull(tagId, "tagId must not be null");
        return taskRepository.findAll()
            .stream()
            .filter(contact -> contact.getTags().stream().anyMatch(tag -> tag.getId().equals(tagId)))
            .count();
    }
}
