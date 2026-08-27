# Behaviors: Dependency Updates (java-parent, spring-services, nextjs-app-layer)

## Build & dependency resolution

### Backend resolves against java-parent 1.2.1

- **Given** `backend/pom.xml` declares `com.open-elements:java-parent` version `1.2.1`
- **When** `./mvnw clean verify` runs
- **Then** the build resolves the parent from Maven Central and completes successfully
- **And** no `git-commit-id`, `pomchecker` or Spotless goal executes, because none of them is bound
  outside the `full-build` profile

### springdoc version is inherited, not pinned

- **Given** the `<springdoc.version>` property and the explicit `<version>` on
  `springdoc-openapi-starter-webmvc-ui` have been removed from `backend/pom.xml`
- **When** `./mvnw dependency:list` runs
- **Then** `springdoc-openapi-starter-webmvc-ui` resolves to 2.8.17, inherited from the parent's
  `dependencyManagement`

### The bare spring-services coordinate is gone

- **Given** `backend/pom.xml` after the upgrade
- **When** its dependency declarations are inspected
- **Then** no dependency on `com.open-elements:spring-services` (the reactor pom) exists
- **And** `spring-services-core`, `spring-services-search` and `spring-services-dbbackup` are declared
  without explicit versions, managed by the imported `spring-services-bom` 1.3.1

### spring-services-mcp is not on the classpath

- **Given** the upgraded backend
- **When** `./mvnw dependency:tree` is inspected
- **Then** `com.open-elements:spring-services-mcp` does not appear at any depth
- **And** the twelve classes in `com.openelements.spring.base.mcp` come solely from
  `backend/src/main/java`

### Unused feature modules stay off the classpath

- **Given** neither `spring-services-slack` nor `spring-services-email` is declared
- **When** `./mvnw dependency:tree` is inspected
- **Then** neither `com.slack.api:slack-api-client` nor `spring-boot-starter-mail` appears

### Frontend lockfile moves to 0.7.1

- **Given** `frontend/package.json` requires `@open-elements/nextjs-app-layer` `^0.7.1`
- **When** `pnpm install` runs
- **Then** `pnpm-lock.yaml` records version 0.7.1
- **And** the resolved versions of `next`, `next-auth`, `react`, `react-dom` and `@open-elements/ui`
  are unchanged

## Application wiring

### The application boots with starter-only wiring

- **Given** `CrmApplication` declares neither `@EntityScan` nor `@EnableJpaRepositories` nor
  `@Import(FullSpringServiceConfig.class)`
- **When** the Spring context starts
- **Then** the context loads without error
- **And** both CRM entities (`com.openelements.crm.*`) and library entities
  (`com.openelements.spring.base.*`) are registered in the single persistence unit

### Library repositories are injectable

- **Given** the started context
- **When** a bean requiring `UserRepository`, `TagRepository` or `CommentRepository` is resolved
- **Then** the repository is injected successfully

### MCP wiring survives the removal of the other imports

- **Given** `CrmApplication` still declares `@Import(McpConfiguration.class)`
- **And** `openelements.mcp.enabled=true`
- **When** the context starts
- **Then** the MCP server beans are created from CRM's own `com.openelements.spring.base.mcp` classes

### Search activates from module presence alone

- **Given** `spring-services-search` is on the classpath
- **And** no `openelements.meilisearch.enabled` property is set
- **When** the context starts
- **Then** `SearchAutoConfiguration` applies and the Meilisearch beans exist
- **And** the CRM-side `SearchConfiguration` beans (`searchIndexExecutor`, `crmScopedKey`, the five
  `IndexSettings`) are wired against them

### Db-backup activates from module presence alone

- **Given** `spring-services-dbbackup` is on the classpath
- **And** `openelements.db-backup.api-token` is blank
- **When** the context starts
- **Then** the context loads without error and `/admin/backup` reports the service as not configured

### Hibernate validates against the new schema

- **Given** `spring.jpa.hibernate.ddl-auto=validate`
- **And** the seven library tables live in `oe_spring_services`
- **When** the application starts
- **Then** schema validation passes and no table is created or altered by Hibernate

## Migration V36

### Fresh database: the full timeline lands in the right schemas

- **Given** an empty PostgreSQL 17 database
- **When** Flyway applies `V1` through `V36`
- **Then** `users`, `api_keys`, `audit_log`, `comments`, `settings`, `tags` and `webhooks` exist in
  `oe_spring_services`
- **And** none of those seven tables exists in `public`
- **And** all CRM tables and join tables remain in `public`

### Existing database: data survives the move

- **Given** a database at `V35` containing rows in `users`, `tags`, `comments` and `audit_log`
- **When** `V36` is applied
- **Then** every row is still present and unchanged, now readable via the qualified table names
- **And** the row counts before and after the migration are identical

### Cross-schema foreign keys stay intact

- **Given** a database at `V35` with an opportunity referencing a user, a company referencing a tag,
  and a comment attached to a company
- **When** `V36` is applied
- **Then** all eleven foreign key constraints listed in the design still exist and are valid
- **And** `public.opportunities.owner_id` now references `oe_spring_services.users(id)`

### Referential integrity is still enforced after the move

- **Given** the migrated database
- **When** an insert into `public.opportunity_tags` names a `tag_id` that does not exist in
  `oe_spring_services.tags`
- **Then** the insert fails with a foreign key violation

### Cascade deletes still work across the schema boundary

- **Given** a tag in `oe_spring_services.tags` linked to a company via `public.company_tags`
- **When** the tag row is deleted
- **Then** the `company_tags` row is removed by `ON DELETE CASCADE`

### The migration is atomic

- **Given** a database where one of the seven tables cannot be moved
- **When** `V36` runs
- **Then** the whole migration rolls back, no table has moved, and the `oe_spring_services` schema is
  not left behind as a partial state

### Flyway history stays in public

- **Given** the migrated database
- **When** `flyway_schema_history` is located
- **Then** it is in the `public` schema
- **And** it contains a row for version 36

## Rollback

### rollback.sql restores the pre-migration layout

- **Given** a database on which `V36` has been applied
- **When** `rollback.sql` is executed
- **Then** all seven tables are back in `public`, the `oe_spring_services` schema is dropped, and the
  `V36` row is removed from `flyway_schema_history`
- **And** all data and all eleven foreign key constraints are intact

### The rolled-back database boots the previous version

- **Given** a database restored by `rollback.sql`
- **When** the 1.2.0 application image starts
- **Then** Flyway validation passes and the application boots

### Forgetting the history row breaks the rollback

- **Given** the tables have been moved back to `public` but the `V36` row is still in
  `flyway_schema_history`
- **When** the 1.2.0 application image starts
- **Then** Flyway aborts with "Detected applied migration not resolved locally: 36"
- **And** the application does not start

_(This scenario documents the failure the rollback procedure must avoid; it is verified during the dev
rehearsal, not as an automated test.)_

## Tests and the guard

### Test SQL targets the qualified tables

- **Given** the test suite after the upgrade
- **When** any test issues `JdbcTemplate` SQL against a library table
- **Then** the statement names the table as `oe_spring_services.<table>`
- **And** no test relies on a `search_path` setting to resolve it

### Truncation still isolates tests

- **Given** a test that has written rows to CRM tables and to library tables
- **When** the `@AfterEach` truncate runs
- **Then** all listed tables in both schemas are emptied in a single `TRUNCATE` statement
- **And** the following test starts from a clean database

### The guard test rejects an unqualified new migration

- **Given** a migration file `V37__example.sql` containing `INSERT INTO users (...)`
- **When** the migration guard test runs
- **Then** it fails, naming the offending file and table

### The guard test accepts a qualified new migration

- **Given** a migration file `V37__example.sql` containing
  `INSERT INTO oe_spring_services.users (...)`
- **When** the migration guard test runs
- **Then** it passes

### The guard test ignores historical migrations

- **Given** the existing migrations `V1` through `V35`, several of which reference `users`, `tags` and
  `comments` unqualified
- **When** the migration guard test runs
- **Then** it passes, because the rule applies only to versions greater than 35

### A CRM table with a library-like name is not falsely flagged

- **Given** a migration with version > 35 that references `public.company_tags` or `opportunity_tags`
- **When** the migration guard test runs
- **Then** it passes, because neither is one of the seven library tables

## Frontend session behaviour

### A fresh session lasts eight hours

- **Given** `createAppLayerAuth` is called with only `issuer`, `clientId` and `clientSecret`
- **When** a user signs in
- **Then** the session cookie carries a `maxAge` of 8 hours rather than the previous 30 days

### A dead refresh token forces re-authentication

- **Given** a session whose refresh token has been revoked at Authentik
- **When** the user requests a protected page
- **Then** the token endpoint returns 4xx, the session is marked `RefreshTokenError`, and the
  middleware redirects to `/login`
- **And** the user is not admitted to an app shell whose API calls would all return 401

### A transient IdP failure does not end the session

- **Given** a valid, unexpired access token
- **When** the refresh call fails with a 5xx, a network error, or a timeout
- **Then** the existing token is kept, the request succeeds, and the refresh is retried later
- **And** the session is only failed once the access token has actually expired

### Concurrent refreshes do not log the user out

- **Given** several requests arrive simultaneously while the access token is inside the refresh window
- **When** they each trigger a refresh
- **Then** exactly one call reaches the token endpoint and all requests share its result
- **And** refresh-token rotation at Authentik does not invalidate any of them

### Short-lived tokens are polled often enough

- **Given** the verification step finds Authentik issues access tokens with a lifetime under two
  minutes for the `open-crm` client
- **When** the fix is applied
- **Then** `OERootLayout` receives a `refetchInterval` of roughly half that lifetime (minimum ~15 s)
- **And** the browser notices an expired session without waiting the default 120 s

### A sufficiently long token lifetime needs no change

- **Given** the verification step finds a token lifetime of two minutes or more
- **When** the frontend is deployed
- **Then** no `refetchInterval` is passed and the default 120 s poll remains

## Production verification

### The smoke pass covers every affected subsystem

- **Given** the upgraded application running in Coolify production
- **When** the operator works through the smoke list
- **Then** login succeeds, global search returns results, a comment can be created and read, the audit
  log lists entries including the new action, `/admin/backup` reports the sidecar healthy, and the MCP
  connector answers

### A post-migration backup still contains the library tables

- **Given** the migration has been applied in production
- **When** a fresh backup is triggered from `/admin/backup` and the dump is inspected
- **Then** the dump contains all seven `oe_spring_services` tables with their data
- **And** if it does not, the backup configuration is treated as a release blocker

### No stale instance runs against the moved schema

- **Given** a Coolify deploy of the new version
- **When** the backend container is replaced
- **Then** no 1.2.0 instance remains running against the same database at any point
