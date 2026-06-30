package com.openelements.crm.contact.csvimport;

import java.util.List;
import java.util.Map;

/**
 * Parsed CSV content ready for import preview or commit.
 *
 * @param delimiter  detected field separator ({@code ,} or {@code ;})
 * @param columns    column names (synthetic names when the file has no header row)
 * @param totalRows  number of data rows in the file
 * @param rows       all data rows; {@code null} entries mark malformed rows
 * @param sampleRows first up to three rows for UI preview
 */
public record ParsedCsv(
    char delimiter,
    List<String> columns,
    int totalRows,
    List<Map<String, String>> rows,
    List<Map<String, String>> sampleRows
) {
}
