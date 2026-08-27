package com.openelements.crm.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards the {@code oe_spring_services} schema split (spec 115, migration V36). Since V36 moved
 * the seven spring-services library tables out of {@code public}, any later migration that names
 * one of them unqualified would either fail or silently target the wrong object once {@code public}
 * is no longer on the {@code search_path}. This test fails the build the moment such a migration is
 * added.
 *
 * <p>Migrations at version {@value #LIBRARY_SCHEMA_BOUNDARY_VERSION} or below are exempt: V1–V35
 * legitimately created and manipulated those tables in {@code public} before the move, and V36 is
 * the move itself — {@code ALTER TABLE users SET SCHEMA …} necessarily names the bare tables while
 * they are still in {@code public}. Everything from V37 onward runs after the move and must qualify.
 *
 * <p>This is a plain file-scan test with no Spring context — it reads the migration SQL directly
 * from the resource directory relative to the module root.
 */
class MigrationSchemaQualificationGuardTest {

    /**
     * Migrations up to and including this version are exempt. This is the version of the schema-move
     * migration ({@code V36}) itself: it and everything before it predate or perform the move and
     * legitimately reference the bare (still-{@code public}) table names. Only later migrations must
     * qualify with {@code oe_spring_services.}.
     */
    private static final int LIBRARY_SCHEMA_BOUNDARY_VERSION = 36;

    private static final String LIBRARY_SCHEMA = "oe_spring_services";

    /** The seven tables that spring-services 1.3.x places in {@code oe_spring_services}. */
    private static final Set<String> LIBRARY_TABLES =
        Set.of("users", "api_keys", "audit_log", "comments", "settings", "tags", "webhooks");

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    /**
     * Matches any of the seven library table names as a standalone SQL identifier, optionally
     * preceded by the {@code oe_spring_services.} qualifier. The leading {@code (?<![\w])} and
     * trailing {@code (?![\w])} boundaries (word chars include {@code _}) ensure that CRM join
     * tables such as {@code company_tags}, {@code opportunity_tags} or {@code task_comments} — whose
     * names merely end in a library table name after an underscore — are never matched. When group 1
     * (the qualifier) is absent, the reference is unqualified and therefore a violation.
     */
    private static final Pattern LIBRARY_TABLE_REFERENCE = Pattern.compile(
        "(?<![\\w])(" + LIBRARY_SCHEMA + "\\.)?"
            + "(" + String.join("|", LIBRARY_TABLES) + ")(?![\\w])",
        Pattern.CASE_INSENSITIVE);

    @Test
    void allNewMigrationsQualifyLibraryTables() throws IOException {
        final var offenders = new TreeMap<String, List<String>>();
        try (Stream<Path> files = Files.list(MIGRATION_DIR)) {
            for (final Path file : files.filter(p -> p.getFileName().toString().endsWith(".sql")).toList()) {
                final int version = versionOf(file.getFileName().toString());
                if (version <= LIBRARY_SCHEMA_BOUNDARY_VERSION) {
                    continue;
                }
                final List<String> refs = unqualifiedLibraryTableRefs(Files.readString(file));
                if (!refs.isEmpty()) {
                    offenders.put(file.getFileName().toString(), refs);
                }
            }
        }
        assertTrue(offenders.isEmpty(),
            () -> "Migrations with version > " + LIBRARY_SCHEMA_BOUNDARY_VERSION
                + " must qualify spring-services tables as " + LIBRARY_SCHEMA
                + ".<table>. Offenders: " + offenders);
    }

    @Test
    void rejectsAnUnqualifiedNewMigration() {
        assertEquals(List.of("users"),
            violationsFor(37, "INSERT INTO users (id, name) VALUES (1, 'x');"));
    }

    @Test
    void acceptsAQualifiedNewMigration() {
        assertEquals(List.of(),
            violationsFor(37, "INSERT INTO oe_spring_services.users (id, name) VALUES (1, 'x');"));
    }

    @Test
    void ignoresHistoricalMigrations() {
        assertEquals(List.of(),
            violationsFor(35, "INSERT INTO users (id) VALUES (1); UPDATE tags SET name = 'x';"));
    }

    @Test
    void doesNotFlagCrmTablesWithLibraryLikeNames() {
        assertEquals(List.of(),
            violationsFor(37,
                "INSERT INTO public.company_tags (tag_id) VALUES (1);"
                    + " INSERT INTO opportunity_tags (tag_id) VALUES (2);"
                    + " INSERT INTO task_comments (comment_id) VALUES (3);"));
    }

    @Test
    void ignoresTableNamesMentionedOnlyInComments() {
        assertEquals(List.of(),
            violationsFor(37, "-- this migration does not touch users or tags\n"
                + "/* nor comments */\nSELECT 1;"));
    }

    /** Applies the version gate, then returns the unqualified library-table references in the SQL. */
    private static List<String> violationsFor(final int version, final String sql) {
        if (version <= LIBRARY_SCHEMA_BOUNDARY_VERSION) {
            return List.of();
        }
        return unqualifiedLibraryTableRefs(sql);
    }

    private static List<String> unqualifiedLibraryTableRefs(final String sql) {
        final Matcher matcher = LIBRARY_TABLE_REFERENCE.matcher(stripComments(sql));
        final List<String> violations = new ArrayList<>();
        while (matcher.find()) {
            if (matcher.group(1) == null) {
                violations.add(matcher.group(2).toLowerCase());
            }
        }
        return violations;
    }

    private static String stripComments(final String sql) {
        return sql
            .replaceAll("(?s)/\\*.*?\\*/", " ")
            .replaceAll("--[^\\n]*", " ");
    }

    /** Parses the Flyway version from a {@code V<n>__*.sql} filename, or -1 if it is not versioned. */
    private static int versionOf(final String fileName) {
        final Matcher matcher = Pattern.compile("^V(\\d+)__").matcher(fileName);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }
}
