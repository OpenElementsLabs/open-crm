package com.openelements.crm.company;

import com.openelements.crm.ImageData;
import com.openelements.crm.comment.CommentRepository;
import com.openelements.crm.contact.ContactRepository;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service handling company business logic including CRUD operations, soft-delete, and restore.
 */
@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final CommentRepository commentRepository;

    /**
     * Creates a new CompanyService.
     *
     * @param companyRepository the company repository
     * @param contactRepository the contact repository
     * @param commentRepository the comment repository
     */
    public CompanyService(final CompanyRepository companyRepository,
                          final ContactRepository contactRepository,
                          final CommentRepository commentRepository) {
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository must not be null");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository must not be null");
    }

    /**
     * Creates a new company.
     *
     * @param request the create request
     * @return the created company response
     */
    public CompanyDto create(final CompanyCreateDto request) {
        Objects.requireNonNull(request, "request must not be null");
        final CompanyEntity entity = new CompanyEntity();
        entity.setName(request.name());
        entity.setEmail(request.email());
        entity.setWebsite(request.website());
        entity.setStreet(request.street());
        entity.setHouseNumber(request.houseNumber());
        entity.setZipCode(request.zipCode());
        entity.setCity(request.city());
        entity.setCountry(request.country());
        final CompanyEntity saved = companyRepository.saveAndFlush(entity);
        return CompanyDto.fromEntity(saved, 0, 0);
    }

    /**
     * Returns a company by its ID.
     *
     * @param id the company ID
     * @return the company response
     * @throws ResponseStatusException with 404 if not found
     */
    @Transactional(readOnly = true)
    public CompanyDto getById(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final CompanyEntity entity = findOrThrow(id);
        return toDto(entity);
    }

    /**
     * Updates an existing company.
     *
     * @param id      the company ID
     * @param request the update request
     * @return the updated company response
     * @throws ResponseStatusException with 404 if not found
     */
    public CompanyDto update(final UUID id, final CompanyUpdateDto request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        final CompanyEntity entity = findOrThrow(id);
        entity.setName(request.name());
        entity.setEmail(request.email());
        entity.setWebsite(request.website());
        entity.setStreet(request.street());
        entity.setHouseNumber(request.houseNumber());
        entity.setZipCode(request.zipCode());
        entity.setCity(request.city());
        entity.setCountry(request.country());
        final CompanyEntity saved = companyRepository.saveAndFlush(entity);
        return toDto(saved);
    }

    /**
     * Soft-deletes a company. Fails with 409 if the company still has contacts.
     *
     * @param id the company ID
     * @throws ResponseStatusException with 404 if not found, 409 if contacts exist
     */
    public void delete(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final CompanyEntity entity = findOrThrow(id);
        if (contactRepository.existsByCompanyId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete company: it still has associated contacts. Remove or reassign them first.");
        }
        entity.setDeleted(true);
        companyRepository.saveAndFlush(entity);
    }

    /**
     * Restores a soft-deleted company. Idempotent — restoring a non-deleted company is a no-op.
     *
     * @param id the company ID
     * @return the restored company response
     * @throws ResponseStatusException with 404 if not found
     */
    public CompanyDto restore(final UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        final CompanyEntity entity = findOrThrow(id);
        entity.setDeleted(false);
        final CompanyEntity saved = companyRepository.saveAndFlush(entity);
        return toDto(saved);
    }

    /**
     * Lists companies with pagination, filtering, and sorting.
     *
     * @param name           partial name filter (case-insensitive)
     * @param city           city filter
     * @param country        country filter
     * @param includeDeleted whether to include soft-deleted companies
     * @param pageable       pagination and sorting parameters
     * @return a page of company responses
     */
    @Transactional(readOnly = true)
    public Page<CompanyDto> list(final String name,
                                      final String city,
                                      final String country,
                                      final boolean includeDeleted,
                                      final Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
        Specification<CompanyEntity> spec = Specification.where(null);

        if (!includeDeleted) {
            spec = spec.and((root, query, cb) -> cb.isFalse(root.get("deleted")));
        }
        if (name != null && !name.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        if (city != null && !city.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("city")), city.toLowerCase()));
        }
        if (country != null && !country.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("country")), country.toLowerCase()));
        }

        return companyRepository.findAll(spec, pageable).map(this::toDto);
    }

    private static final Set<String> ALLOWED_LOGO_TYPES = Set.of(
            "image/svg+xml", "image/png", "image/jpeg");

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
        final CompanyEntity entity = findOrThrow(id);
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
        final CompanyEntity entity = findOrThrow(id);
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
        final CompanyEntity entity = findOrThrow(id);
        entity.setLogo(null);
        entity.setLogoContentType(null);
        companyRepository.saveAndFlush(entity);
    }

    private CompanyDto toDto(final CompanyEntity entity) {
        final long contactCount = contactRepository.countByCompanyId(entity.getId());
        final long commentCount = commentRepository.countByCompanyId(entity.getId());
        return CompanyDto.fromEntity(entity, contactCount, commentCount);
    }

    private CompanyEntity findOrThrow(final UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Company not found: " + id));
    }
}
