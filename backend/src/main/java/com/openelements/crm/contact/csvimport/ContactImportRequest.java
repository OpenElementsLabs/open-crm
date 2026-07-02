package com.openelements.crm.contact.csvimport;

import java.util.Map;

/**
 * Request body for CSV import preview and commit endpoints.
 *
 * @param encoding  character set of the uploaded file ({@code UTF-8} or {@code WINDOWS-1252})
 * @param hasHeader {@code true} when the first row contains column names
 * @param mapping   CSV column name to {@link ImportTarget} name; {@code null} for preview without mapping
 */
public record ContactImportRequest(
    String encoding,
    boolean hasHeader,
    Map<String, String> mapping
) {
}
