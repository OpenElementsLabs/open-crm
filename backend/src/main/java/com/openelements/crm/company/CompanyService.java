package com.openelements.crm.company;

import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.task.TaskRepository;
import com.openelements.spring.base.data.AbstractDbBackedDataService;
import com.openelements.spring.base.data.EntityRepository;
import com.openelements.spring.base.data.ImageData;
import com.openelements.spring.base.services.tag.TagEntity;
import com.openelements.spring.base.services.tag.TagRepository;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service handling company business logic including CRUD operations and hard-delete.
 */
@Service
@Transactional
public class CompanyService extends AbstractDbBackedDataService<CompanyEntity, CompanyDto> {

    private static final Set<String> ALLOWED_LOGO_TYPES = Set.of(ImageData.CONTENT_TYPE_SVG, ImageData.CONTENT_TYPE_PNG, ImageData.CONTENT_TYPE_JPEG);

    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;

    public CompanyService(final CompanyRepository companyRepository,
                          final ContactRepository contactRepository,
                          final CommentRepository commentRepository,
                          final TaskRepository taskRepository,
                          final TagRepository tagRepository,
                          final ApplicationEventPublisher eventPublisher) {
        super(eventPublisher);
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.tagRepository = Objects.requireNonNull(tagRepository, "tagRepository must not be null");
    }

    /**
     * Lists companies with pagination, filtering, and sorting.
     *
     * @param name     partial name filter (case-insensitive)
     * @param brevo    filter by Brevo origin (true = only Brevo, false = only non-Brevo, null = all)
     * @param tagIds   filter by tag IDs (AND semantics)
     * @param pageable pagination and sorting parameters
     * @return a page of company responses
     */
    @Transactional(readOnly = true)
    public Page<CompanyDto> list(final String name,
                                 final Boolean brevo,
                                 final List<UUID> tagIds,
                                 final Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
        Specification<CompanyEntity> spec = Specification.where(null);

        if (name != null && !name.isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        if (brevo != null) {
            if (brevo) {
                spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("brevoCompanyId")));
            } else {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("brevoCompanyId")));
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

        return companyRepository.findAll(spec, pageable).map(e -> toData(e));
    }

    /**
     * Lists all companies matching the given filters without pagination.
     *
     * @param name   partial name filter (case-insensitive)
     * @param brevo  filter by Brevo origin
     * @param tagIds filter by tag IDs (AND semantics)
     * @return list of all matching company DTOs
     */
    @Transactional(readOnly = true)
    public List<CompanyDto> listAll(final String name,
                                    final Boolean brevo,
                                    final List<UUID> tagIds) {
        Specification<CompanyEntity> spec = Specification.where(null);

        if (name != null && !name.isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        if (brevo != null) {
            if (brevo) {
                spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("brevoCompanyId")));
            } else {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("brevoCompanyId")));
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

        return companyRepository.findAll(spec, Sort.by("name")).stream()
            .map(e -> toData(e))
            .toList();
    }

    /**
     * Uploads or replaces the logo for a company.
     *
     * @param id          the company ID
     * @param data        the image bytes
     * @param contentType the MIME content type
     * @throws ResponseStatusException with 404 if not found, 400 if content type is invalid
     */
    public void uploadLogo(final UUID id, final byte[] data, final String contentType) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        if (!ALLOWED_LOGO_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid content type: " + contentType + ". Allowed: " + ALLOWED_LOGO_TYPES);
        }
        final CompanyEntity entity = companyRepository.findByIdOrThrow(id);
        entity.setLogo(data);
        entity.setLogoContentType(contentType);
        companyRepository.saveAndFlush(entity);
    }

    /**
     * Returns the logo data for a company.
     *
     * @param id the company ID
     * @return the image data
     * @throws ResponseStatusException with 404 if not found or no logo exists
     */
    @Transactional(readOnly = true)
    public ImageData getLogo(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final CompanyEntity entity = companyRepository.findByIdOrThrow(id);
        if (entity.getLogo() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No logo for company: " + id);
        }
        return new ImageData(entity.getLogo(), entity.getLogoContentType());
    }

    /**
     * Removes the logo from a company.
     *
     * @param id the company ID
     * @throws ResponseStatusException with 404 if not found
     */
    public void deleteLogo(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final CompanyEntity entity = companyRepository.findByIdOrThrow(id);
        entity.setLogo(null);
        entity.setLogoContentType(null);
        companyRepository.saveAndFlush(entity);
    }

    @Override
    protected CompanyEntity createDetachedEntity() {
        return new CompanyEntity();
    }

    @Override
    protected void updateEntity(CompanyEntity entity, CompanyDto data) {
        entity.setName(data.name());
        entity.setEmail(data.email());
        entity.setWebsite(data.website());
        entity.setStreet(data.street());
        entity.setHouseNumber(data.houseNumber());
        entity.setZipCode(data.zipCode());
        entity.setCity(data.city());
        entity.setCountry(data.country());
        entity.setPhoneNumber(data.phoneNumber());
        entity.setDescription(data.description());
        entity.setBankName(data.bankName());
        entity.setBic(data.bic());
        entity.setIban(data.iban());
        entity.setVatId(data.vatId());
        final Set<TagEntity> tags = Optional.ofNullable(data.tagIds())
            .orElse(List.of()).stream()
            .map(id -> tagRepository.findByIdOrThrow(id))
            .collect(Collectors.toUnmodifiableSet());
        entity.setTags(tags);
    }

    @Override
    protected CompanyDto toData(CompanyEntity entity) {
        return new CompanyDto(entity.getId(),
            entity.getName(),
            entity.getEmail(),
            entity.getWebsite(),
            entity.getStreet(),
            entity.getHouseNumber(),
            entity.getZipCode(),
            entity.getCity(),
            entity.getCountry(),
            entity.getPhoneNumber(),
            entity.getDescription(),
            entity.getBankName(),
            entity.getBic(),
            entity.getIban(),
            entity.getVatId(),
            entity.getLogo() != null,
            entity.getBrevoCompanyId() != null,
            contactRepository.countByCompanyId(entity.getId()),
            commentRepository.countByCompanyId(entity.getId()),
            entity.getTags().stream().map(TagEntity::getId).toList(),
            entity.getCreatedAt(),
            entity.getUpdatedAt());
    }

    @Override
    protected EntityRepository<CompanyEntity> getRepository() {
        return companyRepository;
    }

    public long countWithTag(UUID tagId) {
        Objects.requireNonNull(tagId, "tagId must not be null");
        return companyRepository.findAll()
            .stream()
            .filter(contact -> contact.getTags().stream().anyMatch(tag -> tag.getId().equals(tagId)))
            .count();
    }
}
