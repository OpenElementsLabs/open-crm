package com.openelements.crm.contact;

import java.util.function.Function;

/**
 * Enum representing exportable columns for contact CSV export.
 */
public enum ContactExportColumn {

    FIRST_NAME("firstName", dto -> dto.firstName()),
    LAST_NAME("lastName", dto -> dto.lastName()),
    EMAIL("email", dto -> dto.email()),
    POSITION("position", dto -> dto.position()),
    GENDER("gender", dto -> dto.gender() != null ? dto.gender().name() : null),
    LINKED_IN_URL("linkedInUrl", dto -> dto.linkedInUrl()),
    PHONE_NUMBER("phoneNumber", dto -> dto.phoneNumber()),
    COMPANY_NAME("companyName", dto -> dto.companyName()),
    BIRTHDAY("birthday", dto -> dto.birthday() != null ? dto.birthday().toString() : null),
    LANGUAGE("language", dto -> dto.language() != null ? dto.language().name() : null),
    BREVO("brevo", dto -> String.valueOf(dto.brevo())),
    CREATED_AT("createdAt", dto -> dto.createdAt() != null ? dto.createdAt().toString() : null),
    UPDATED_AT("updatedAt", dto -> dto.updatedAt() != null ? dto.updatedAt().toString() : null);

    private final String header;
    private final Function<ContactDto, String> extractor;

    ContactExportColumn(final String header, final Function<ContactDto, String> extractor) {
        this.header = header;
        this.extractor = extractor;
    }

    /**
     * Returns the CSV header name for this column.
     */
    public String getHeader() {
        return header;
    }

    /**
     * Extracts the value for this column from a ContactDto.
     */
    public String extract(final ContactDto dto) {
        final String value = extractor.apply(dto);
        return value != null ? value : "";
    }
}
