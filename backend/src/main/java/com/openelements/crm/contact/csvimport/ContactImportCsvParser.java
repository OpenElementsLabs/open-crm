package com.openelements.crm.contact.csvimport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses uploaded CSV files for contact import. Kept separate from {@link ContactImportService}
 * because {@link MultipartFile} is HTTP-specific.
 */
@Component
public class ContactImportCsvParser {

    static final int MAX_ROWS = 5_000;
    static final long MAX_FILE_SIZE_BYTES = 20L * 1024L * 1024L;

    private static final Charset CHARSET_WINDOWS_1252 = Charset.forName("windows-1252");
    private static final String NORMALIZED_UTF_8 = normalizeEncodingName(StandardCharsets.UTF_8.name());
    private static final String NORMALIZED_WINDOWS_1252 = normalizeEncodingName(CHARSET_WINDOWS_1252.name());
    private static final Set<String> SUPPORTED_ENCODING_NAMES = Set.of(
        NORMALIZED_UTF_8,
        NORMALIZED_WINDOWS_1252
    );

    public ParsedCsv parse(final MultipartFile file, final ContactImportRequest request) {
        if (file == null || file.isEmpty()) {
            throw badRequest("empty_file", "The uploaded file has no content");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw payloadTooLarge("file_too_large", "The uploaded file exceeds the 20 MB limit");
        }
        final Charset charset = resolveCharset(request.encoding());
        try {
            final String content = stripBomIfUtf8(new String(file.getBytes(), charset));
            return parseCsv(content, request.hasHeader());
        } catch (final IOException e) {
            throw badRequest("unreadable_file", "The file could not be read with the selected encoding");
        }
    }

    private ParsedCsv parseCsv(final String content, final boolean hasHeader) throws IOException {
        if (content.isBlank()) {
            throw badRequest("empty_file", "The uploaded file has no rows");
        }

        final char delimiter = detectDelimiter(firstNonEmptyLine(content));
        final CSVFormat baseFormat = CSVFormat.DEFAULT.builder()
            .setDelimiter(delimiter)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();

        if (hasHeader) {
            return parseWithHeader(content, baseFormat, delimiter);
        }
        return parseWithoutHeader(content, baseFormat, delimiter);
    }

    private ParsedCsv parseWithHeader(final String content, final CSVFormat baseFormat, final char delimiter)
        throws IOException {
        final CSVFormat format = baseFormat.builder().setHeader().setSkipHeaderRecord(true).build();
        try (CSVParser parser = CSVParser.parse(content, format)) {
            final List<String> columns = parser.getHeaderNames();
            if (columns.isEmpty()) {
                throw badRequest("no_columns", "The file has no columns");
            }
            return collectRows(parser, columns, delimiter);
        } catch (final ResponseStatusException ex) {
            throw ex;
        } catch (final IOException | IllegalArgumentException ex) {
            throw badRequest("parse_error", "The CSV file could not be parsed");
        }
    }

    private ParsedCsv parseWithoutHeader(final String content, final CSVFormat baseFormat, final char delimiter)
        throws IOException {
        try (CSVParser parser = CSVParser.parse(content, baseFormat)) {
            final List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                throw badRequest("no_data_rows", "The file has no data rows");
            }
            final int columnCount = records.getFirst().size();
            final List<String> columns = new ArrayList<>(columnCount);
            for (int i = 0; i < columnCount; i++) {
                columns.add("Spalte " + (i + 1));
            }
            final List<Map<String, String>> rows = new ArrayList<>();
            int rowCount = 0;
            for (final CSVRecord record : records) {
                rowCount++;
                if (rowCount > MAX_ROWS) {
                    throw payloadTooLarge("row_limit_exceeded", "The file exceeds the 5000 row limit");
                }
                if (record.size() != columnCount) {
                    rows.add(null);
                } else {
                    rows.add(toRowMap(columns, record));
                }
            }
            return buildParsedCsv(delimiter, columns, rowCount, rows);
        } catch (final ResponseStatusException ex) {
            throw ex;
        } catch (final IOException | IllegalArgumentException ex) {
            throw badRequest("parse_error", "The CSV file could not be parsed");
        }
    }

    private ParsedCsv collectRows(final CSVParser parser, final List<String> columns, final char delimiter)
        throws IOException {
        final List<Map<String, String>> rows = new ArrayList<>();
        int rowCount = 0;
        for (final CSVRecord record : parser) {
            rowCount++;
            if (rowCount > MAX_ROWS) {
                throw payloadTooLarge("row_limit_exceeded", "The file exceeds the 5000 row limit");
            }
            if (record.size() != columns.size()) {
                rows.add(null);
            } else {
                rows.add(toRowMap(columns, record));
            }
        }
        if (rowCount == 0) {
            throw badRequest("no_data_rows", "The file has no data rows");
        }
        return buildParsedCsv(delimiter, columns, rowCount, rows);
    }

    private static ParsedCsv buildParsedCsv(final char delimiter, final List<String> columns,
                                          final int rowCount, final List<Map<String, String>> rows) {
        final List<Map<String, String>> sampleRows = rows.stream()
            .limit(3)
            .map(row -> row == null ? Map.<String, String>of() : row)
            .toList();
        return new ParsedCsv(delimiter, List.copyOf(columns), rowCount, List.copyOf(rows), sampleRows);
    }

    private static Map<String, String> toRowMap(final List<String> columns, final CSVRecord record) {
        final Map<String, String> row = new LinkedHashMap<>();
        for (final String column : columns) {
            row.put(column, record.get(column));
        }
        return row;
    }

    private static Charset resolveCharset(final String encoding) {
        if (encoding == null || encoding.isBlank()) {
            throw unsupportedEncoding("Encoding is required");
        }
        final String normalized = normalizeEncodingName(encoding);
        if (!SUPPORTED_ENCODING_NAMES.contains(normalized)) {
            throw unsupportedEncoding("Unsupported encoding: " + encoding);
        }
        return NORMALIZED_WINDOWS_1252.equals(normalized) ? CHARSET_WINDOWS_1252 : StandardCharsets.UTF_8;
    }

    private static String normalizeEncodingName(final String encoding) {
        return encoding.trim().toUpperCase().replace('_', '-');
    }

    private static String stripBomIfUtf8(final String content) {
        if (content.startsWith("\uFEFF")) {
            return content.substring(1);
        }
        return content;
    }

    private static String firstNonEmptyLine(final String content) {
        for (final String line : content.split("\\R", -1)) {
            if (!line.isBlank()) {
                return line;
            }
        }
        return content;
    }

    private static char detectDelimiter(final String line) {
        int commas = 0;
        int semicolons = 0;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (!inQuotes) {
                if (c == ',') {
                    commas++;
                } else if (c == ';') {
                    semicolons++;
                }
            }
        }
        return semicolons > commas ? ';' : ',';
    }

    private static ResponseStatusException badRequest(final String error, final String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, error + ": " + detail);
    }

    private static ResponseStatusException payloadTooLarge(final String error, final String detail) {
        return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, error + ": " + detail);
    }

    private static ResponseStatusException unsupportedEncoding(final String detail) {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_encoding: " + detail);
    }
}
