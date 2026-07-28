package com.openelements.crm.opportunity;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.data.AbstractDbBackedDataService;
import com.openelements.spring.base.data.EntityRepository;
import com.openelements.spring.base.services.audit.AuditAction;
import com.openelements.spring.base.services.audit.AuditLogEntity;
import com.openelements.spring.base.services.audit.AuditLogRepository;
import com.openelements.spring.base.services.comment.CommentCreateDto;
import com.openelements.spring.base.services.comment.CommentDto;
import com.openelements.spring.base.services.comment.CommentEntity;
import com.openelements.spring.base.services.comment.CommentRepository;
import com.openelements.spring.base.services.comment.CommentService;
import com.openelements.spring.base.services.tag.TagEntity;
import com.openelements.spring.base.services.tag.TagRepository;
import com.openelements.spring.base.services.user.UserEntity;
import com.openelements.spring.base.services.user.UserRepository;
import com.openelements.spring.base.services.user.UserService;
import jakarta.persistence.criteria.JoinType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service handling opportunity business logic. Extends
 * {@link AbstractDbBackedDataService} to inherit CRUD and the lifecycle events (audit log, search
 * indexing, updates feed, webhooks) shared by all CRM entities, and adds opportunity-specific
 * reference validation, comment handling, list filtering, and tag counting.
 */
@Service
@Transactional
public class OpportunityService extends AbstractDbBackedDataService<OpportunityEntity, OpportunityDto> {

    public static final String COMMENT_ENTITY_TYPE = "OpportunityComment";

    private final OpportunityRepository opportunityRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TagRepository tagRepository;
    private final CommentService commentService;
    private final CommentRepository commentRepository;
    private final AuditLogRepository auditLogRepository;

    public OpportunityService(final OpportunityRepository opportunityRepository,
                              final CompanyRepository companyRepository,
                              final ContactRepository contactRepository,
                              final UserRepository userRepository,
                              final UserService userService,
                              final TagRepository tagRepository,
                              final CommentService commentService,
                              final CommentRepository commentRepository,
                              final AuditLogRepository auditLogRepository,
                              final ApplicationEventPublisher eventPublisher) {
        super(eventPublisher);
        this.opportunityRepository = Objects.requireNonNull(opportunityRepository, "opportunityRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
        this.tagRepository = Objects.requireNonNull(tagRepository, "tagRepository must not be null");
        this.commentService = Objects.requireNonNull(commentService, "commentService must not be null");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
    }

    /**
     * Lists opportunities with pagination and optional filters.
     *
     * @param search    case-insensitive title contains filter
     * @param status    exact status filter
     * @param stage     exact stage filter
     * @param companyId exact company filter
     * @param contactId matches the main <em>or</em> an additional contact
     * @param ownerId   exact owner filter
     * @param tagIds    tag IDs that must all be present (AND semantics)
     * @param pageable  pagination and sorting parameters
     * @return a page of opportunity DTOs
     */
    @Transactional(readOnly = true)
    public Page<OpportunityDto> list(final String search,
                                     final OpportunityStatus status,
                                     final String stage,
                                     final UUID companyId,
                                     final UUID contactId,
                                     final UUID ownerId,
                                     final List<UUID> tagIds,
                                     final Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
        Specification<OpportunityEntity> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            final String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (stage != null && !stage.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("stage"), stage));
        }
        if (companyId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("company").get("id"), companyId));
        }
        if (ownerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId));
        }
        if (contactId != null) {
            spec = spec.and((root, query, cb) -> {
                final var additional = root.join("additionalContacts", JoinType.LEFT);
                query.distinct(true);
                return cb.or(
                    cb.equal(root.get("mainContact").get("id"), contactId),
                    cb.equal(additional.get("id"), contactId));
            });
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
        return opportunityRepository.findAll(spec, pageable).map(this::toData);
    }

    /**
     * Returns every opportunity as a DTO, used by the search bootstrap step. Runs read-only so the
     * lazy relations resolved in {@link #toData} are available.
     *
     * @return all opportunities as DTOs
     */
    @Transactional(readOnly = true)
    public List<OpportunityDto> findAllForIndex() {
        return opportunityRepository.findAll().stream()
            .map(this::toData)
            .toList();
    }

    // -- Comments ------------------------------------------------------------

    /**
     * Lists comments attached to an opportunity, newest first.
     */
    @Transactional(readOnly = true)
    public List<CommentDto> listCommentsOfOpportunity(final UUID opportunityId) {
        Objects.requireNonNull(opportunityId, "opportunityId must not be null");
        final OpportunityEntity opportunity = opportunityRepository.findById(opportunityId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found"));
        return opportunity.getComments().stream()
            .map(c -> commentService.findById(c.getId()).orElseThrow())
            .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
            .toList();
    }

    /**
     * Adds a comment to an opportunity. The comment row and the {@code opportunity_comments} join
     * row are inserted in the same transaction; the author is set to the current user by the library.
     */
    public CommentDto addCommentToOpportunity(final UUID opportunityId, final CommentCreateDto request) {
        Objects.requireNonNull(opportunityId, "opportunityId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final OpportunityEntity opportunity = opportunityRepository.findById(opportunityId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found"));
        final CommentDto saved = commentService.save(new CommentDto(null, request.text(), null, null, null));
        final CommentEntity entity = commentRepository.findByIdOrThrow(saved.id());
        opportunity.getComments().add(entity);
        opportunityRepository.save(opportunity);
        recordCommentAudit(opportunityId, AuditAction.INSERT);
        return saved;
    }

    /**
     * Updates a comment attached to the given opportunity. Returns 404 if the comment is not
     * attached to this opportunity.
     */
    public CommentDto updateCommentOfOpportunity(final UUID opportunityId, final UUID commentId, final CommentCreateDto request) {
        Objects.requireNonNull(opportunityId, "opportunityId must not be null");
        Objects.requireNonNull(commentId, "commentId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        assertCommentBelongsToOpportunity(opportunityId, commentId);
        final CommentDto current = commentService.findById(commentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        final CommentDto saved = commentService.save(
            new CommentDto(commentId, request.text(), current.author(), current.createdAt(), current.updatedAt()));
        recordCommentAudit(opportunityId, AuditAction.UPDATE);
        return saved;
    }

    /**
     * Deletes a comment attached to the given opportunity. Removes both the join row and the
     * comment row.
     */
    public void deleteCommentOfOpportunity(final UUID opportunityId, final UUID commentId) {
        Objects.requireNonNull(opportunityId, "opportunityId must not be null");
        Objects.requireNonNull(commentId, "commentId must not be null");
        assertCommentBelongsToOpportunity(opportunityId, commentId);
        final OpportunityEntity opportunity = opportunityRepository.findByIdOrThrow(opportunityId);
        opportunity.getComments().removeIf(c -> c.getId().equals(commentId));
        opportunityRepository.saveAndFlush(opportunity);
        commentService.delete(commentId);
        recordCommentAudit(opportunityId, AuditAction.DELETE);
    }

    private void recordCommentAudit(final UUID opportunityId, final AuditAction action) {
        final AuditLogEntity entry = new AuditLogEntity();
        entry.setEntityType(COMMENT_ENTITY_TYPE);
        entry.setEntityId(opportunityId);
        entry.setName(opportunityRepository.findById(opportunityId)
            .map(OpportunityEntity::getTitle)
            .filter(t -> t != null && !t.isBlank())
            .orElse("UNKNOWN"));
        entry.setAction(action);
        entry.setUser(userService.getCurrentUserEntity());
        auditLogRepository.save(entry);
    }

    private void assertCommentBelongsToOpportunity(final UUID opportunityId, final UUID commentId) {
        final OpportunityEntity opportunity = opportunityRepository.findById(opportunityId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found"));
        final boolean belongs = opportunity.getComments().stream()
            .anyMatch(c -> c.getId().equals(commentId));
        if (!belongs) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found for this opportunity");
        }
    }

    // -- Tag counting & referential checks -----------------------------------

    /**
     * @param tagId the tag ID
     * @return the number of opportunities carrying the given tag
     */
    @Transactional(readOnly = true)
    public long countWithTag(final UUID tagId) {
        Objects.requireNonNull(tagId, "tagId must not be null");
        return opportunityRepository.findAll().stream()
            .filter(o -> o.getTags().stream().anyMatch(tag -> tag.getId().equals(tagId)))
            .count();
    }

    /**
     * @param companyId the company ID
     * @return {@code true} if any opportunity references the given company
     */
    @Transactional(readOnly = true)
    public boolean existsByCompany(final UUID companyId) {
        Objects.requireNonNull(companyId, "companyId must not be null");
        return opportunityRepository.existsByCompanyId(companyId);
    }

    /**
     * @param contactId the contact ID
     * @return {@code true} if the given contact is the main contact of any opportunity
     */
    @Transactional(readOnly = true)
    public boolean existsByMainContact(final UUID contactId) {
        Objects.requireNonNull(contactId, "contactId must not be null");
        return opportunityRepository.existsByMainContactId(contactId);
    }

    // -- Lifecycle -----------------------------------------------------------

    @Override
    public void delete(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final OpportunityEntity opportunity = opportunityRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + id));
        final List<UUID> commentIds = new ArrayList<>(
            opportunity.getComments().stream().map(CommentEntity::getId).toList());
        opportunity.getComments().clear();
        opportunityRepository.saveAndFlush(opportunity);
        commentIds.forEach(commentService::delete);
        // Convert to DTO inside the still-active @Transactional method so lazy relations resolve,
        // then defer to the lib's delete(D) so pre/post delete events fire (audit, search, feed).
        super.delete(toData(opportunity));
    }

    @Override
    protected OpportunityEntity createDetachedEntity() {
        return new OpportunityEntity();
    }

    @Override
    protected void updateEntity(final OpportunityEntity entity, final OpportunityDto data) {
        final CompanyEntity company = companyRepository.findById(data.companyId())
            .orElseThrow(() -> badRequest("Unknown companyId: " + data.companyId()));
        final ContactEntity mainContact = contactRepository.findById(data.mainContactId())
            .orElseThrow(() -> badRequest("Unknown mainContactId: " + data.mainContactId()));
        final UUID ownerId = data.owner() == null ? null : data.owner().id();
        if (ownerId == null) {
            throw badRequest("ownerId must not be null");
        }
        final UserEntity owner = userRepository.findById(ownerId)
            .orElseThrow(() -> badRequest("Unknown ownerId: " + ownerId));

        final List<UUID> additionalIds = Optional.ofNullable(data.additionalContactIds()).orElse(List.of());
        if (additionalIds.contains(data.mainContactId())) {
            throw badRequest("main contact must not be listed as additional contact");
        }
        final Set<ContactEntity> additionalContacts = additionalIds.stream()
            .map(id -> contactRepository.findById(id)
                .orElseThrow(() -> badRequest("Unknown additional contact: " + id)))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        final Set<TagEntity> tags = Optional.ofNullable(data.tagIds()).orElse(List.of()).stream()
            .map(id -> tagRepository.findById(id)
                .orElseThrow(() -> badRequest("Unknown tag: " + id)))
            .collect(Collectors.toSet());

        entity.setTitle(data.title());
        entity.setStage(data.stage());
        entity.setStatus(data.status() == null ? OpportunityStatus.OPEN : data.status());
        entity.setProduct(data.product());
        entity.setEstimatedValue(data.estimatedValue());
        entity.setCompany(company);
        entity.setMainContact(mainContact);
        entity.setOwner(owner);
        entity.setAdditionalContacts(additionalContacts);
        entity.setTags(tags);
    }

    @Override
    protected OpportunityDto toData(final OpportunityEntity entity) {
        final CompanyEntity company = entity.getCompany();
        final ContactEntity mainContact = entity.getMainContact();
        return new OpportunityDto(
            entity.getId(),
            entity.getTitle(),
            entity.getStage(),
            entity.getStatus(),
            entity.getProduct(),
            entity.getEstimatedValue(),
            company == null ? null : company.getId(),
            company == null ? null : company.getName(),
            mainContact == null ? null : mainContact.getId(),
            mainContact == null ? null : contactDisplayName(mainContact),
            entity.getAdditionalContacts().stream().map(ContactEntity::getId).toList(),
            OpportunityUserMapper.toUserDto(entity.getOwner()),
            entity.getTags().stream().map(TagEntity::getId).toList(),
            entity.getComments().size(),
            entity.getCreatedAt(),
            entity.getUpdatedAt());
    }

    @Override
    protected EntityRepository<OpportunityEntity> getRepository() {
        return opportunityRepository;
    }

    static String contactDisplayName(final ContactEntity contact) {
        final StringBuilder sb = new StringBuilder();
        appendPart(sb, contact.getTitle());
        appendPart(sb, contact.getFirstName());
        appendPart(sb, contact.getLastName());
        return sb.toString().trim();
    }

    private static void appendPart(final StringBuilder sb, final String part) {
        if (part != null && !part.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(part);
        }
    }

    private static ResponseStatusException badRequest(final String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
