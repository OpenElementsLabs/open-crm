# TODO

## URL ↔ Filter Synchronization for Contact List

The contact list should fully synchronize URL parameters with the filter UI:

- All filter values (firstName, lastName, email, companyId, language, sort) should be readable from URL parameters
- Filter changes by the user should update the URL in real-time
- This enables sharing filtered views via URL

**Context:** Deferred from spec 009 (contact-company cross-navigation). Currently, only `companyId` is read from the URL
on initial load, and the filter dropdown does not reflect the URL-driven filter value.

## Testcontainers Integration Tests

Add Testcontainers-based integration tests that run against a real PostgreSQL database. This would catch schema
mismatches between Flyway migrations and JPA entity mappings (like the `@Lob`/`BYTEA` mismatch from Spec 013) that are
invisible in H2-based tests with `ddl-auto: create-drop`.

**Context:** Identified during the grill session for Spec 017 (Fix image column type). Currently, tests run against H2
with Hibernate-generated schema, so Flyway migration correctness is never verified in tests.

## Company Duplicate Merging

Provide a way to detect and merge duplicate companies. This is needed because the Brevo import creates new companies
from the `COMPANY` text field on contacts without matching against existing company names — duplicates are expected and
acceptable during import. A separate merge feature will allow cleaning these up later.

**Context:** Deferred from the Brevo import integration spec.
