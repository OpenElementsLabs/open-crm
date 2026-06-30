package com.openelements.crm.contact.csvimport;

import java.util.List;
import java.util.Map;

/**
 * Response from the CSV import preview endpoint.
 *
 * @param delimiter      detected field separator ({@code ,} or {@code ;})
 * @param columns        column names from the CSV header or synthetic names
 * @param totalRows      number of data rows in the file
 * @param sampleRows     first rows as raw column maps for the mapping step
 * @param sampleContacts mapped and validated preview rows; {@code null} when no mapping was sent
 */
public record ContactImportPreviewResponse(
    String delimiter,
    List<String> columns,
    int totalRows,
    List<Map<String, String>> sampleRows,
    List<ContactPreviewDto> sampleContacts
) {
}
