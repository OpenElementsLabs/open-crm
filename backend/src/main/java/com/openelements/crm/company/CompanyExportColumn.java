package com.openelements.crm.company;

import java.util.function.Function;

/**
 * Enum representing exportable columns for company CSV export.
 */
public enum CompanyExportColumn {

    NAME("name", dto -> dto.name()),
    EMAIL("email", dto -> dto.email()),
    WEBSITE("website", dto -> dto.website()),
    STREET("street", dto -> dto.street()),
    HOUSE_NUMBER("houseNumber", dto -> dto.houseNumber()),
    ZIP_CODE("zipCode", dto -> dto.zipCode()),
    CITY("city", dto -> dto.city()),
    COUNTRY("country", dto -> dto.country()),
    CONTACT_COUNT("contactCount", dto -> String.valueOf(dto.contactCount())),
    COMMENT_COUNT("commentCount", dto -> String.valueOf(dto.commentCount())),
    BREVO("brevo", dto -> String.valueOf(dto.brevo())),
    CREATED_AT("createdAt", dto -> dto.createdAt() != null ? dto.createdAt().toString() : null),
    UPDATED_AT("updatedAt", dto -> dto.updatedAt() != null ? dto.updatedAt().toString() : null);

    private final String header;
    private final Function<CompanyDto, String> extractor;

    CompanyExportColumn(final String header, final Function<CompanyDto, String> extractor) {
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
     * Extracts the value for this column from a CompanyDto.
     */
    public String extract(final CompanyDto dto) {
        final String value = extractor.apply(dto);
        return value != null ? value : "";
    }
}
