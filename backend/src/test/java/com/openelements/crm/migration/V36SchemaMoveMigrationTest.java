package com.openelements.crm.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Verifies migration {@code V36} against an <em>existing</em> database — the timeline the normal
 * test suite cannot reproduce, because that suite always migrates a fresh container straight to the
 * latest version. This test drives Flyway to {@code V35}, seeds rows into the library tables while
 * they are still in {@code public}, then migrates to {@code V36} and asserts the data survived the
 * move into {@code oe_spring_services}.
 *
 * <p>Covers the behaviours "Existing database: data survives the move", "Fresh database: the tables
 * land in the right schemas" and "Flyway history stays in public". Cross-schema foreign-key
 * enforcement and cascade behaviour are exercised at runtime by the opportunity/comment integration
 * tests, which operate across the schema boundary after the move.
 *
 * <p>Uses its own container (not the shared {@code AbstractDbTest} one) because it needs to control
 * the Flyway target version rather than migrate straight to head.
 */
class V36SchemaMoveMigrationTest {

    private static final String LIBRARY_SCHEMA = "oe_spring_services";

    private static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void startContainer() {
        postgres = new PostgreSQLContainer<>("postgres:17-alpine");
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @Test
    void v36MovesLibraryTablesIntoTheDedicatedSchemaAndPreservesData() throws SQLException {
        migrateTo("35");

        final int usersBefore;
        final int tagsBefore;
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.executeUpdate(
                "INSERT INTO users (sub, user_name, name) "
                    + "VALUES ('sub-a', 'alice', 'Alice'), ('sub-b', 'bob', 'Bob')");
            st.executeUpdate(
                "INSERT INTO tags (name, color) VALUES ('lead', '#ff0000'), ('vip', '#00ff00')");
            // Count in public before the move (a migration may have pre-seeded a system user row).
            usersBefore = countRows(conn, "public.users");
            tagsBefore = countRows(conn, "public.tags");
        }

        migrateTo("36");

        try (Connection conn = connect()) {
            // Data survived and is now readable under the qualified names, with identical counts.
            assertEquals(usersBefore, countRows(conn, LIBRARY_SCHEMA + ".users"),
                "users row count unchanged by the move");
            assertEquals(tagsBefore, countRows(conn, LIBRARY_SCHEMA + ".tags"),
                "tags row count unchanged by the move");

            // The seven library tables now live in oe_spring_services, and no longer in public.
            for (final String table :
                new String[] {"users", "api_keys", "audit_log", "comments", "settings", "tags", "webhooks"}) {
                assertTrue(tableExistsInSchema(conn, LIBRARY_SCHEMA, table),
                    () -> table + " should exist in " + LIBRARY_SCHEMA);
                assertFalse(tableExistsInSchema(conn, "public", table),
                    () -> table + " should no longer exist in public");
            }

            // Flyway's own history table stays in public and records the move.
            assertTrue(tableExistsInSchema(conn, "public", "flyway_schema_history"),
                "flyway history stays in public");
            assertEquals(1, countRows(conn,
                    "public.flyway_schema_history WHERE version = '36'"),
                "a V36 history row exists");
        }
    }

    private static void migrateTo(final String version) {
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion(version))
            .load()
            .migrate();
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private static int countRows(final Connection conn, final String from) throws SQLException {
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + from)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static boolean tableExistsInSchema(
            final Connection conn, final String schema, final String table) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, schema, table, new String[] {"TABLE"})) {
            return rs.next();
        }
    }
}
