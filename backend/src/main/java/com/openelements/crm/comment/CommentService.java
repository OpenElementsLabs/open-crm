package com.openelements.crm.comment;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.company.CompanyService;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.ContactService;
import com.openelements.crm.task.TaskEntity;
import com.openelements.crm.task.TaskRepository;
import java.util.Objects;
import java.util.UUID;

import com.openelements.spring.base.data.AbstractDbBackedDataService;
import com.openelements.spring.base.data.EntityRepository;
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
public class CommentService extends AbstractDbBackedDataService<CommentEntity, CommentDto> {

    private final CommentRepository commentRepository;
    private final UserService userService;
    private CompanyRepository companyRepository;
    private ContactRepository contactRepository;
    private TaskRepository taskRepository;

    public CommentService(final CommentRepository commentRepository,
                          final CompanyRepository companyRepository,
                          final ContactRepository contactService,
                          final TaskRepository taskRepository,
                          final UserService userService,
                          final ApplicationEventPublisher eventPublisher) {
        super(eventPublisher);
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactService, "contactService must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
    }

  @Transactional(readOnly = true)
    public Page<CommentDto> listByCompany(final UUID companyId, final Pageable pageable) {
        Objects.requireNonNull(companyId, "companyId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found: " + companyId);
        }
        return commentRepository.findByCompanyId(companyId, pageable).map(CommentDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> listByContact(final UUID contactId, final Pageable pageable) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (!contactRepository.existsById(contactId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found: " + contactId);
        }
        return commentRepository.findByContactId(contactId, pageable).map(CommentDto::fromEntity);
    }

   @Transactional(readOnly = true)
    public Page<CommentDto> listByTask(final UUID taskId, final Pageable pageable) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (!taskRepository.existsById(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId);
        }
        return commentRepository.findByTaskId(taskId, pageable).map(CommentDto::fromEntity);
    }

    @Override
    protected CommentEntity createDetachedEntity() {
        return new CommentEntity();
    }

    @Override
    protected void updateEntity(CommentEntity entity, CommentDto data) {
        entity.setText(data.text());
        entity.setAuthor(data.author());
        if(data.companyId() != null) {
            final CompanyEntity company = companyRepository.findById(data.companyId()).orElseThrow(() -> new IllegalArgumentException("Company not found: " + data.companyId()));
            entity.setCompany(company);
        }
        if(data.contactId() != null) {
            final ContactEntity contact = contactRepository.findById(data.contactId()).orElseThrow(() -> new IllegalArgumentException("Contact not found: " + data.contactId()));
            entity.setContact(contact);
        }
        if(data.taskId() != null) {
            final TaskEntity task = taskRepository.findById(data.taskId()).orElseThrow(() -> new IllegalArgumentException("Task not found: " + data.taskId()));
            entity.setTask(task);
        }
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
