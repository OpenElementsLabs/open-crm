package com.openelements.crm.contact.csvimport;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    @NonNull List<String> columns,
    int totalRows,
    @NonNull List<@Nullable Map<String, String>> rows,
    @NonNull List<Map<String, String>> sampleRows
) {
    public ParsedCsv {
        columns = List.copyOf(Objects.requireNonNullElse(columns, List.of()));
        rows = List.copyOf(Objects.requireNonNullElse(rows, List.of()));
        sampleRows = List.copyOf(Objects.requireNonNullElse(sampleRows, List.of()));
    }
}
