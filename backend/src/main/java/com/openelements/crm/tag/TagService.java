package com.openelements.crm.tag;

import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.task.TaskRepository;
import com.openelements.crm.webhook.WebhookEvent;
import com.openelements.crm.webhook.WebhookEventType;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final TaskRepository taskRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TagService(final TagRepository tagRepository,
                      final CompanyRepository companyRepository,
                      final ContactRepository contactRepository,
                      final TaskRepository taskRepository,
                      final ApplicationEventPublisher eventPublisher) {
        this.tagRepository = Objects.requireNonNull(tagRepository, "tagRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    public Page<TagDto> findAll(final Pageable pageable, final boolean includeCounts) {
        final Page<TagEntity> page = tagRepository.findAll(pageable);
        if (includeCounts) {
            return page.map(entity -> TagDto.fromEntity(entity,
                    tagRepository.countActiveCompaniesByTagId(entity.getId()),
                    tagRepository.countContactsByTagId(entity.getId()),
                    tagRepository.countActiveTasksByTagId(entity.getId())));
        }
        return page.map(TagDto::fromEntity);
    }

    public TagDto findById(final UUID id) {
        return tagRepository.findById(id)
                .map(TagDto::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }

    public TagDto create(final TagCreateDto dto) {
        if (tagRepository.existsByNameIgnoreCase(dto.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag with this name already exists");
        }
        final TagEntity entity = new TagEntity();
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setColor(dto.color());
        final TagDto result = TagDto.fromEntity(tagRepository.saveAndFlush(entity));
        eventPublisher.publishEvent(new WebhookEvent(WebhookEventType.TAG_CREATED, result.id(), result));
        return result;
    }

    public TagDto update(final UUID id, final TagCreateDto dto) {
        final TagEntity entity = tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
        tagRepository.findByNameIgnoreCase(dto.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag with this name already exists");
                });
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setColor(dto.color());
        final TagDto result = TagDto.fromEntity(tagRepository.saveAndFlush(entity));
        eventPublisher.publishEvent(new WebhookEvent(WebhookEventType.TAG_UPDATED, result.id(), result));
        return result;
    }

    public void delete(final UUID id) {
        final TagEntity tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
        // Remove tag from all companies and contacts before deleting
        companyRepository.findAll().stream()
                .filter(c -> c.getTags().contains(tag))
                .forEach(c -> {
                    c.getTags().remove(tag);
                    companyRepository.save(c);
                });
        contactRepository.findAll().stream()
                .filter(c -> c.getTags().contains(tag))
                .forEach(c -> {
                    c.getTags().remove(tag);
                    contactRepository.save(c);
                });
        taskRepository.findAll().stream()
                .filter(t -> t.getTags().contains(tag))
                .forEach(t -> {
                    t.getTags().remove(tag);
                    taskRepository.save(t);
                });
        tagRepository.delete(tag);
        eventPublisher.publishEvent(new WebhookEvent(WebhookEventType.TAG_DELETED, id, null));
    }

    public Set<TagEntity> resolveTagIds(final List<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        final List<TagEntity> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more tag IDs do not exist");
        }
        return new HashSet<>(tags);
    }
}
