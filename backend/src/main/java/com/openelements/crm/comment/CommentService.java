package com.openelements.crm.comment;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.task.TaskEntity;
import com.openelements.crm.task.TaskRepository;
import com.openelements.spring.base.data.AbstractDbBackedDataService;
import com.openelements.spring.base.data.EntityRepository;
import com.openelements.spring.base.security.user.UserService;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service handling comment business logic.
 */
@Service
@Transactional
public class CommentService extends AbstractDbBackedDataService<CommentEntity, CommentDto> {

    private final CommentRepository commentRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final TaskRepository taskRepository;

    public CommentService(@NonNull final CommentRepository commentRepository,
                          @NonNull final CompanyRepository companyRepository,
                          @NonNull final ContactRepository contactService,
                          @NonNull final TaskRepository taskRepository,
                          @NonNull final UserService userService,
                          @NonNull final ApplicationEventPublisher eventPublisher) {
        super(eventPublisher);
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactService, "contactService must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
    }

    @NonNull
    @Transactional(readOnly = true)
    public Page<CommentDto> listByCompany(@NonNull final UUID companyId, @NonNull final Pageable pageable) {
        Objects.requireNonNull(companyId, "companyId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found: " + companyId);
        }
        return commentRepository.findByCompanyId(companyId, pageable).map(e -> toData(e));
    }

    @NonNull
    @Transactional(readOnly = true)
    public Page<CommentDto> listByContact(@NonNull final UUID contactId, @NonNull final Pageable pageable) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (!contactRepository.existsById(contactId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found: " + contactId);
        }
        return commentRepository.findByContactId(contactId, pageable).map(e -> toData(e));
    }

    @NonNull
    @Transactional(readOnly = true)
    public Page<CommentDto> listByTask(@NonNull final UUID taskId, @NonNull final Pageable pageable) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (!taskRepository.existsById(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId);
        }
        return commentRepository.findByTaskId(taskId, pageable).map(e -> toData(e));
    }

    @Override
    protected CommentEntity createDetachedEntity() {
        return new CommentEntity();
    }

    @Override
    protected void updateEntity(CommentEntity entity, CommentDto data) {
        entity.setText(data.text());
        entity.setAuthor(data.author());
        final CompanyEntity company = Optional.ofNullable(data.companyId())
            .map(id -> companyRepository.findByIdOrThrow(data.companyId()))
            .orElse(null);
        entity.setCompany(company);

        final ContactEntity contact = Optional.ofNullable(data.contactId())
            .map(id -> contactRepository.findByIdOrThrow(data.contactId()))
            .orElse(null);
        entity.setContact(contact);

        final TaskEntity task = Optional.ofNullable(data.taskId())
            .map(id -> taskRepository.findByIdOrThrow(id))
            .orElse(null);
        entity.setTask(task);
    }

    @Override
    protected CommentDto toData(CommentEntity entity) {
        return new CommentDto(
            entity.getId(),
            entity.getText(),
            entity.getAuthor(),
            entity.getCompany() != null ? entity.getCompany().getId() : null,
            entity.getContact() != null ? entity.getContact().getId() : null,
            entity.getTask() != null ? entity.getTask().getId() : null,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    @Override
    protected EntityRepository<CommentEntity> getRepository() {
        return commentRepository;
    }
}
