package com.openelements.crm.contact;

import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.task.TaskRepository;
import com.openelements.spring.base.data.AbstractDbBackedDataService;
import com.openelements.spring.base.data.EntityRepository;
import com.openelements.spring.base.security.user.ImageData;
import com.openelements.spring.base.services.tag.TagRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Service handling contact business logic including CRUD operations and company validation.
 */
@Service
@Transactional
public class ContactService extends AbstractDbBackedDataService<ContactEntity, ContactDto> {

    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ContactService(final ContactRepository contactRepository,
                          final CompanyRepository companyRepository,
                          final CommentRepository commentRepository,
                          final TaskRepository taskRepository,
                          final TagRepository tagRepository,
                          final ApplicationEventPublisher eventPublisher) {
        super((eventPublisher));
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.tagRepository = Objects.requireNonNull(tagRepository, "tagService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
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
                    final Join<ContactEntity, SocialLinkEntity> socialLinksJoin = root.join("socialLinks", JoinType.LEFT);
                    query.distinct(true);
                    return cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(companyJoin.get("name")), pattern),
                        cb.like(cb.lower(socialLinksJoin.get("value")), pattern)
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
                    final Join<ContactEntity, SocialLinkEntity> socialLinksJoin = root.join("socialLinks", JoinType.LEFT);
                    query.distinct(true);
                    return cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(companyJoin.get("name")), pattern),
                        cb.like(cb.lower(socialLinksJoin.get("value")), pattern)
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


    private ContactEntity findOrThrow(final UUID id) {
        return contactRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Contact not found: " + id));
    }

    @Override
    protected ContactEntity createDetachedEntity() {
        return new ContactEntity();
    }

    @Override
    protected EntityRepository<ContactEntity> getRepository() {
        return contactRepository;
    }

    @Override
    protected void updateEntity(ContactEntity entity, ContactDto data) {
        entity.setTitle(data.title());
        entity.setFirstName(data.firstName());
        entity.setLastName(data.lastName());
        entity.setEmail(data.email());
        entity.setPosition(data.position());
        entity.setGender(data.gender());
        entity.setPhoneNumber(data.phoneNumber());
        entity.setDescription(data.description());
        entity.setBirthday(data.birthday());
        entity.setLanguage(data.language());


        entity.getSocialLinks().clear();
        if (data.socialLinks() != null) {
            for (final SocialLinkDto linkDto : data.socialLinks()) {
                final SocialNetworkType networkType = SocialNetworkType.valueOf(linkDto.networkType());
                final SocialNetworkType.ResolvedLink resolved = networkType.resolve(linkDto.value());
                final SocialLinkEntity linkEntity = new SocialLinkEntity();
                linkEntity.setNetworkType(networkType);
                linkEntity.setValue(resolved.value());
                linkEntity.setUrl(resolved.url());
                entity.getSocialLinks().add(linkEntity);
            }
        }

        if (data.companyId() != null) {
            final CompanyEntity company = companyRepository.findById(data.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Company not found: " + data.companyId()));
            entity.setCompany(company);
        } else {
            entity.setCompany(null);
        }

        entity.setTags(Set.of());
        if (data.tagIds() != null) {
            entity.setTags(tagRepository.findAll(data.tagIds()));
        }
    }

    @Override
    protected ContactDto toData(ContactEntity entity) {
        final long commentCount = commentRepository.countByContactId(entity.getId());
        return ContactDto.fromEntity(entity, commentCount);
    }

}
