package com.openelements.crm.contact.csvimport;

import com.openelements.crm.contact.ContactCreateDto;
import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.SocialLinkCreateDto;
import com.openelements.crm.contact.SocialLinkDto;
import com.openelements.crm.contact.SocialNetworkType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validates mapped CSV rows and imports contacts row by row with partial success.
 */
@Service
public class ContactImportService {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final ContactImportRowSaver rowSaver;

    public ContactImportService(final ContactImportRowSaver rowSaver) {
        this.rowSaver = Objects.requireNonNull(rowSaver, "rowSaver must not be null");
    }

    public ContactImportPreviewResponse preview(final ParsedCsv parsed, final ContactImportRequest request) {
        final List<ContactPreviewDto> sampleContacts = buildSampleContacts(parsed, request.mapping());
        return new ContactImportPreviewResponse(
            String.valueOf(parsed.delimiter()),
            parsed.columns(),
            parsed.totalRows(),
            parsed.sampleRows(),
            sampleContacts
        );
    }

    public ImportResult commit(final ParsedCsv parsed, final ContactImportRequest request) {
        final Map<String, ImportTarget> mapping = validateMapping(request.mapping(), parsed.columns(), true);
        int createdCount = 0;
        final List<ImportFailureDto> failures = new ArrayList<>();
        for (int i = 0; i < parsed.rows().size(); i++) {
            final int rowNumber = i + 1;
            final Map<String, String> rawRow = parsed.rows().get(i);
            if (rawRow == null) {
                failures.add(new ImportFailureDto(rowNumber, null, "malformed_row", Map.of()));
                continue;
            }
            final TransformResult transformResult = transformAndValidate(rawRow, mapping);
            if (!transformResult.errors().isEmpty()) {
                final RowError first = transformResult.errors().getFirst();
                failures.add(new ImportFailureDto(rowNumber, first.field(), first.reason(), rawRow));
                continue;
            }
            try {
                rowSaver.save(toContactDto(transformResult.contact()));
                createdCount++;
            } catch (final RuntimeException ex) {
                failures.add(new ImportFailureDto(rowNumber, null, "save_failed", rawRow));
            }
        }
        return new ImportResult(createdCount, failures.size(), failures);
    }

    record TransformResult(ContactPreviewFields contact, List<RowError> errors) {
    }

    TransformResult transformAndValidate(final Map<String, String> rawRow, final Map<String, ImportTarget> mapping) {
        final EnumMap<ImportTarget, String> values = new EnumMap<>(ImportTarget.class);
        for (final Map.Entry<String, ImportTarget> entry : mapping.entrySet()) {
            final String cell = rawRow.getOrDefault(entry.getKey(), "");
            values.put(entry.getValue(), cell == null ? "" : cell.trim());
        }

        final List<RowError> errors = new ArrayList<>();
        for (final Map.Entry<ImportTarget, String> entry : values.entrySet()) {
            final ImportTarget target = entry.getKey();
            final String value = entry.getValue();
            if ((target == ImportTarget.FIRST_NAME || target == ImportTarget.LAST_NAME) && value.isBlank()) {
                errors.add(new RowError(target.fieldName(), "required"));
            } else if (!value.isBlank() && value.length() > target.maxLength()) {
                errors.add(new RowError(target.fieldName(), "too_long"));
            }
        }

        final String email = values.getOrDefault(ImportTarget.EMAIL, "");
        if (!email.isBlank() && !isValidEmail(email)) {
            errors.add(new RowError("email", "invalid"));
        }

        normalizeSocialValue(values, ImportTarget.LINKEDIN_URL, SocialNetworkType.LINKEDIN);
        normalizeSocialValue(values, ImportTarget.WEBSITE_URL, SocialNetworkType.WEBSITE);

        validateSocial(values.get(ImportTarget.LINKEDIN_URL), "linkedInUrl", SocialNetworkType.LINKEDIN, errors);
        validateSocial(values.get(ImportTarget.WEBSITE_URL), "websiteUrl", SocialNetworkType.WEBSITE, errors);

        final String linkedInUrl = blankToNull(values.get(ImportTarget.LINKEDIN_URL));
        final String websiteUrl = blankToNull(values.get(ImportTarget.WEBSITE_URL));

        final ContactPreviewFields contact = new ContactPreviewFields(
            blankToNull(values.get(ImportTarget.TITLE)),
            blankToNull(values.get(ImportTarget.FIRST_NAME)),
            blankToNull(values.get(ImportTarget.LAST_NAME)),
            blankToNull(email),
            blankToNull(values.get(ImportTarget.POSITION)),
            blankToNull(values.get(ImportTarget.PHONE_NUMBER)),
            linkedInUrl,
            websiteUrl
        );
        return new TransformResult(contact, List.copyOf(errors));
    }

    private List<ContactPreviewDto> buildSampleContacts(final ParsedCsv parsed, final Map<String, String> mapping) {
        if (mapping == null || mapping.isEmpty()) {
            return null;
        }
        final Map<String, ImportTarget> validated = validateMapping(mapping, parsed.columns(), true);
        final List<ContactPreviewDto> previews = new ArrayList<>();
        for (int i = 0; i < parsed.sampleRows().size(); i++) {
            final Map<String, String> rawRow = parsed.sampleRows().get(i);
            final TransformResult result = transformAndValidate(rawRow, validated);
            previews.add(new ContactPreviewDto(i + 1, result.contact(), result.errors()));
        }
        return previews;
    }

    private Map<String, ImportTarget> validateMapping(final Map<String, String> mapping,
                                                      final List<String> columns,
                                                      final boolean required) {
        if (mapping == null || mapping.isEmpty()) {
            if (required) {
                throw badRequest("missing_mapping", "Column mapping is required");
            }
            return Map.of();
        }

        final Set<String> columnSet = Set.copyOf(columns);
        final Map<String, ImportTarget> result = new LinkedHashMap<>();
        final Map<ImportTarget, String> reverse = new EnumMap<>(ImportTarget.class);

        for (final Map.Entry<String, String> entry : mapping.entrySet()) {
            if (!columnSet.contains(entry.getKey())) {
                throw badRequest("unknown_column", "Mapping references unknown column: " + entry.getKey());
            }
            final ImportTarget target;
            try {
                target = ImportTarget.valueOf(entry.getValue());
            } catch (final IllegalArgumentException ex) {
                throw badRequest("invalid_target", "Unknown mapping target: " + entry.getValue());
            }
            if (reverse.containsKey(target)) {
                throw badRequest("duplicate_target", "Target " + target.name() + " is mapped more than once");
            }
            reverse.put(target, entry.getKey());
            result.put(entry.getKey(), target);
        }

        if (required || !mapping.isEmpty()) {
            if (!reverse.containsKey(ImportTarget.FIRST_NAME)) {
                throw badRequest("missing_first_name", "firstName must be mapped exactly once");
            }
            if (!reverse.containsKey(ImportTarget.LAST_NAME)) {
                throw badRequest("missing_last_name", "lastName must be mapped exactly once");
            }
        }
        return result;
    }

    private static ContactDto toContactDto(final ContactPreviewFields fields) {
        final List<SocialLinkCreateDto> socialLinks = new ArrayList<>();
        if (fields.linkedInUrl() != null) {
            socialLinks.add(new SocialLinkCreateDto("LINKEDIN", fields.linkedInUrl()));
        }
        if (fields.websiteUrl() != null) {
            socialLinks.add(new SocialLinkCreateDto("WEBSITE", fields.websiteUrl()));
        }
        final ContactCreateDto createDto = new ContactCreateDto(
            fields.title(),
            fields.firstName(),
            fields.lastName(),
            fields.email(),
            fields.position(),
            null,
            socialLinks,
            fields.phoneNumber(),
            null,
            null,
            null,
            null,
            null
        );
        return new ContactDto(
            null,
            createDto.title(),
            createDto.firstName(),
            createDto.lastName(),
            createDto.email(),
            createDto.position(),
            createDto.gender(),
            createDto.socialLinks().stream().map(SocialLinkDto::fromCreateDto).toList(),
            createDto.phoneNumber(),
            createDto.description(),
            createDto.companyId(),
            null,
            0,
            false,
            createDto.birthday(),
            false,
            false,
            createDto.language(),
            createDto.tagIds(),
            null,
            null
        );
    }

    private static void validateSocial(final String value, final String fieldName,
                                       final SocialNetworkType networkType, final List<RowError> errors) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            networkType.resolve(value.trim());
        } catch (final RuntimeException ex) {
            errors.add(new RowError(fieldName, "invalid"));
        }
    }

    private static void normalizeSocialValue(final EnumMap<ImportTarget, String> values,
                                           final ImportTarget target,
                                           final SocialNetworkType networkType) {
        final String raw = values.get(target);
        if (raw == null || raw.isBlank()) {
            return;
        }
        final String extracted = extractSocialUrl(raw, networkType);
        if (extracted != null) {
            values.put(target, extracted);
        } else if (hasRecognizableUrl(raw)) {
            values.put(target, "");
        } else {
            values.put(target, raw.trim());
        }
    }

    /**
     * CRM exports often pack several URLs in one cell (e.g. LinkedIn and website comma-separated).
     * Picks the first segment that validates for the requested network type.
     */
    static String extractSocialUrl(final String raw, final SocialNetworkType networkType) {
        for (final String candidate : splitUrlCandidates(raw)) {
            try {
                return networkType.resolve(candidate).value();
            } catch (final RuntimeException ignored) {
                // try next segment
            }
        }
        return null;
    }

    private static boolean hasRecognizableUrl(final String raw) {
        for (final String candidate : splitUrlCandidates(raw)) {
            if (!looksLikeUrl(candidate)) {
                continue;
            }
            for (final SocialNetworkType type : SocialNetworkType.values()) {
                try {
                    type.resolve(candidate);
                    return true;
                } catch (final RuntimeException ignored) {
                    // try next type
                }
            }
        }
        return false;
    }

    private static boolean looksLikeUrl(final String candidate) {
        return candidate.startsWith("http://") || candidate.startsWith("https://");
    }

    private static List<String> splitUrlCandidates(final String raw) {
        final List<String> candidates = new ArrayList<>();
        for (final String part : raw.split(",")) {
            final String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                candidates.add(trimmed);
            }
        }
        if (candidates.isEmpty()) {
            candidates.add(raw.trim());
        }
        return candidates;
    }

    private static boolean isValidEmail(final String email) {
        return VALIDATOR.validate(new EmailHolder(email)).isEmpty();
    }

    private record EmailHolder(@NotBlank @Email String email) {
    }

    private static String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static ResponseStatusException badRequest(final String error, final String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, error + ": " + detail);
    }
}
