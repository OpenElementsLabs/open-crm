# Behaviors: Open Elements Dependency Updates

Scenarios are grouped by the three commits of the change. Scenarios marked *(manual)* are verified once
during implementation and recorded in the pull request; all others become automated tests.

## Backend — dependency resolution

### Swagger resolves to a single version across the stack

- **Given** `backend/pom.xml` declares `com.open-elements:java-parent` at version `1.3.0`
- **When** `./mvnw dependency:tree -Dincludes=io.swagger.core.v3,org.webjars:swagger-ui` is run
- **Then** `swagger-annotations-jakarta`, `swagger-models-jakarta` and `swagger-core-jakarta` all resolve
  to `2.2.47`
- **And** `org.webjars:swagger-ui` resolves to `5.32.2`
- *(manual)*

### The annotations no longer lose mediation to spring-services-core

- **Given** `spring-services-core:1.3.1` declares `swagger-annotations-jakarta:2.2.29` at depth 1
- **When** the dependency tree is resolved with `java-parent` 1.3.0
- **Then** the managed `2.2.47` applies instead of the nearer `2.2.29`, because `dependencyManagement` is
  consulted before nearest-wins mediation
- *(manual)*

### No other managed version moves

- **Given** the parent bump is the only change in `backend/pom.xml`
- **When** the effective POM is resolved
- **Then** `spring-boot-dependencies` is still `3.5.14` and `testcontainers-bom` is still `2.0.5`
- **And** no plugin version in the consumer has been changed
- *(manual)*

### The build stays green

- **Given** the parent is bumped to `1.3.0`
- **When** `./mvnw clean verify` is run
- **Then** the build succeeds and all existing tests pass

## Backend — OpenAPI endpoint

### The API documentation resolves a schema

- **Given** the application is running with `java-parent` 1.3.0 on the classpath
- **When** `GET /v3/api-docs` is requested by an authenticated user
- **Then** the response status is `200`
- **And** the body is a JSON document containing an `openapi` version field and a non-empty `paths` object

### Schema resolution does not fail on a split Swagger stack

- **Given** the OpenAPI document is generated for controllers annotated with `@Schema`, `@Operation` and
  `@ApiResponse`
- **When** springdoc resolves the models
- **Then** no `NoSuchMethodError` is raised
- **And** the request completes without a `500`

### Component schemas for the annotated DTOs are present

- **Given** `GET /v3/api-docs` returned successfully
- **When** the document is inspected
- **Then** it contains component schemas for the DTOs exposed by the contact and company controllers

## Frontend — Markdown editor toolbar

### The contact description form offers exactly four actions

- **Given** the contact form is rendered
- **When** the `MarkdownEditor` for the description field appears
- **Then** its toolbar offers Bold, Italic, Strikethrough and Link
- **And** it offers no further actions

### The company description form offers exactly four actions

- **Given** the company form is rendered
- **When** the `MarkdownEditor` for the description field appears
- **Then** its toolbar offers Bold, Italic, Strikethrough and Link
- **And** it offers no further actions

### The toolbar is declared, not inherited

- **Given** `@open-elements/ui` defaults `toolbar` to `["bold", "italic"]`
- **When** either form is rendered
- **Then** the four actions appear regardless of the library default, because the prop is passed explicitly

### Task lists cannot be created

- **Given** `"taskList"` is absent from the declared toolbar
- **When** the user types `[ ] ` at the start of a line, or presses `Mod-Shift-9`
- **Then** no task list is created

## Frontend — Markdown round-trip

### Structured Markdown survives loading into the editor

- **Given** a stored description containing a heading, a bullet list, an ordered list, a blockquote, a code
  block and a horizontal rule
- **When** the value is loaded into `MarkdownEditor`
- **Then** the value reported back through `onChange` is unchanged from the stored value
- **And** no `onChange` fires merely from mounting the component

### Structured Markdown renders as structure in the view

- **Given** the same stored content
- **When** it is rendered by `MarkdownView`
- **Then** the output contains heading, list, blockquote and code-block elements rather than a single
  flattened paragraph

### Plain content is unaffected

- **Given** a description containing only paragraphs, bold, italic, strikethrough and links
- **When** it is loaded into the editor and reported back
- **Then** the value is byte-identical to the stored value

### Task-list checkboxes are read-only

- **Given** stored content containing `- [x]` and `- [ ]` items
- **And** `MarkdownView` is rendered without an `onChange` prop
- **When** the reader clicks a checkbox
- **Then** the checkbox state reverts and no change is reported

## Frontend — backend proxy wiring

### The bearer token from the session is attached

- **Given** an authenticated session carrying an access token
- **When** a request passes through the proxy route
- **Then** the upstream request carries `Authorization: Bearer <token>`

### A spoofed Authorization header cannot override the session token

- **Given** the incoming request carries its own `Authorization` header
- **When** the proxy forwards the request
- **Then** the upstream request carries the token derived from the session, not the incoming header

### The backend URL falls back when unset

- **Given** `BACKEND_URL` is not set in the environment
- **When** the proxy forwards a request
- **Then** the upstream URL is built against `http://localhost:8080`

### All four HTTP methods are served

- **Given** the route module `src/app/api/[...path]/route.ts`
- **When** its exports are inspected
- **Then** `GET`, `POST`, `PUT` and `DELETE` are all exported and bound to the same handler

### Session cookies never reach the backend

- **Given** the incoming request carries a `Cookie` header
- **When** the proxy forwards the request
- **Then** the upstream request carries no `Cookie` header
- **And** it carries no `Host` header taken from the incoming request

## Frontend — backend proxy contract

These scenarios assert the library's behaviour on purpose, so that a future bump of
`@open-elements/nextjs-app-layer` that changes it fails here.

### The request body is streamed rather than buffered

- **Given** a `POST` request with a body
- **When** the proxy forwards it
- **Then** the upstream `fetch` is called with `duplex: "half"` and the request body as a stream
- **And** the incoming body has not been consumed by the handler

### Content-Length is forwarded

- **Given** an incoming request that declares `Content-Length`
- **When** the proxy forwards it
- **Then** the upstream request carries the same `Content-Length`, so the backend can reject an oversized
  upload before reading the body

### Range and conditional headers are forwarded

- **Given** an incoming `GET` carrying `Range` and `If-None-Match`
- **When** the proxy forwards it
- **Then** both headers appear on the upstream request

### An upstream partial response passes through

- **Given** the backend answers with `206 Partial Content`
- **When** the proxy relays the response
- **Then** the client receives status `206` and the partial body

### A GET carries no body

- **Given** a `GET` request
- **When** the proxy forwards it
- **Then** the upstream request has no body and no `duplex` option

## Supply-chain policy

### A fresh third-party package is blocked

- **Given** `minimumReleaseAge: 10080` is configured in `frontend/pnpm-workspace.yaml`
- **And** a third-party dependency resolves to a version published less than seven days ago
- **When** `pnpm install` resolves the dependency graph
- **Then** the install fails with `ERR_PNPM_NO_MATURE_MATCHING_VERSION`
- *(manual)*

### Open Elements packages are exempt

- **Given** the same configuration with `minimumReleaseAgeExclude: ["@open-elements/*"]`
- **And** `@open-elements/ui@0.10.0` and `@open-elements/nextjs-app-layer@0.8.0` were published less than
  seven days ago
- **When** `pnpm install` resolves the dependency graph
- **Then** the install succeeds
- *(manual)*

### The exemption is selective, not a global switch

- **Given** the wildcard exemption is active
- **When** a third-party package inside the quarantine window is added
- **Then** it is still blocked
- *(manual)*

### A frozen-lockfile install re-verifies the policy

- **Given** a committed `pnpm-lock.yaml` containing an entry inside the quarantine window
- **When** `pnpm install --frozen-lockfile` runs in CI or in the Docker build
- **Then** the install fails with `ERR_PNPM_MINIMUM_RELEASE_AGE_VIOLATION` unless the exemption is present
  in `pnpm-workspace.yaml`
- *(manual)*

### The Docker build sees the policy

- **Given** `frontend/Dockerfile` copies `package.json`, `pnpm-lock.yaml` and `pnpm-workspace.yaml` before
  installing
- **When** the frontend image is built
- **Then** `pnpm install --frozen-lockfile` succeeds with the committed lockfile

### CI sees the policy

- **Given** `.github/workflows/build.yml` runs `pnpm install --frozen-lockfile` after a repository checkout
- **When** the frontend job runs
- **Then** the install succeeds with the committed lockfile

## Application version

### The application version is untouched

- **Given** this change bumps dependencies but not the application
- **When** `./check-versions.sh` runs
- **Then** it reports `OK — 1.12.0-SNAPSHOT`
- **And** `backend/pom.xml` and `frontend/package.json` still carry identical versions

## Rollback

### Reverting the frontend commit restores the previous pair

- **Given** the frontend commit bumped both libraries and the lockfile together
- **When** that commit is reverted
- **Then** `@open-elements/ui` is back at `^0.9.0` and `@open-elements/nextjs-app-layer` at `^0.7.1`
- **And** no peer-dependency conflict exists at any point

### Reverting the backend commit restores the previous parent

- **Given** the backend commit bumped only the parent version and added a test
- **When** that commit is reverted
- **Then** `backend/pom.xml` is back at `java-parent` 1.2.1
- **And** the Swagger stack returns to the split state the project is in today
