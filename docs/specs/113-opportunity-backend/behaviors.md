# Behaviors: Opportunity (Deal) — Backend

## Opportunity creation

### Create with all fields

- **Given** an existing company, two existing contacts, an existing user, and an existing tag
- **When** `POST /api/opportunities` is called with title "Muster-Bank – CRA Support & Care", stage "Angebot", status `WON`, product "Support & Care", estimatedValue 25000.00, the company ID, contact A as main contact, contact B as additional contact, the user as owner, and the tag ID
- **Then** the response is `201 Created` with an `OpportunityDto` containing all submitted values, the generated UUID, `companyName`/`mainContactName` denormalized, the owner as nested `UserDto`, `commentCount` 0, and `createdAt`/`updatedAt` set

### Create with minimal fields applies defaults

- **Given** an existing company and an existing contact, and the request is authenticated as user U
- **When** `POST /api/opportunities` is called with only title, companyId, and mainContactId
- **Then** the response is `201 Created` with status `OPEN`, owner = user U (current user), and stage, product, and estimatedValue null

### Stage accepts any string

- **Given** a valid create payload
- **When** the stage is set to an arbitrary value like "Kanban-Spalte-42" that is not in the frontend value list
- **Then** the opportunity is created with exactly that stage value (no validation against a value list)

### Missing title is rejected

- **Given** a valid create payload
- **When** the title is missing or blank
- **Then** the response is `400 Bad Request` and no opportunity is created

### Missing company or main contact is rejected

- **Given** a create payload without `companyId` (or without `mainContactId`)
- **When** `POST /api/opportunities` is called
- **Then** the response is `400 Bad Request`

### Unknown referenced IDs are rejected

- **Given** a create payload whose `companyId`, `mainContactId`, `ownerId`, a `tagIds` entry, or an `additionalContactIds` entry does not exist
- **When** `POST /api/opportunities` is called
- **Then** the response is `400 Bad Request` and no opportunity is created

### Main contact must not be an additional contact

- **Given** a create payload where the same contact UUID appears as `mainContactId` and inside `additionalContactIds`
- **When** `POST /api/opportunities` is called
- **Then** the response is `400 Bad Request`

### Negative estimated value is rejected

- **Given** a valid create payload
- **When** `estimatedValue` is -1
- **Then** the response is `400 Bad Request`

### Estimated value with more than two decimal places is rejected

- **Given** a valid create payload
- **When** `estimatedValue` is 100.999
- **Then** the response is `400 Bad Request`

### Contact of a different company is accepted

- **Given** company A and a contact belonging to company B (or to no company)
- **When** an opportunity is created for company A with that contact as main contact
- **Then** the response is `201 Created` (consistency is a frontend warning only)

## Opportunity retrieval

### Get by ID

- **Given** an existing opportunity with comments and tags
- **When** `GET /api/opportunities/{id}` is called
- **Then** the response is `200 OK` with the full `OpportunityDto` including `commentCount` and `tagIds`

### Get unknown ID

- **When** `GET /api/opportunities/{id}` is called with a non-existing UUID
- **Then** the response is `404 Not Found`

### Paginated list

- **Given** 25 opportunities
- **When** `GET /api/opportunities?page=0&size=20` is called
- **Then** the response is `200 OK` with 20 items and a stable `VIA_DTO` page envelope (`totalElements` 25)

### List filters

- **Given** opportunities with different statuses, stages, companies, contacts, owners, and tags
- **When** the list is filtered by `search` (title substring, case-insensitive), `status`, `stage`, `companyId`, `contactId`, `ownerId`, or `tagIds`
- **Then** only matching opportunities are returned; the `contactId` filter matches opportunities where the contact is the main **or** an additional contact

## Opportunity update

### Full update

- **Given** an existing opportunity
- **When** `PUT /api/opportunities/{id}` is called with changed title, stage, status `LOST`, product, value, a different main contact, different additional contacts, a different owner, and different tags
- **Then** the response is `200 OK` with all values replaced and `updatedAt` newer than before

### Update validation mirrors create

- **Given** an existing opportunity
- **When** an update payload violates a create rule (blank title, unknown reference, main contact in additional contacts, negative value, missing status or owner)
- **Then** the response is `400 Bad Request` and the opportunity is unchanged

### Update unknown ID

- **When** `PUT /api/opportunities/{id}` is called with a non-existing UUID
- **Then** the response is `404 Not Found`

## Opportunity deletion

### Admin deletes an opportunity

- **Given** an existing opportunity with two comments, one tag, and one additional contact, and the caller has the APP-ADMIN role
- **When** `DELETE /api/opportunities/{id}` is called
- **Then** the response is `204 No Content`; the opportunity, its comment join rows, its comments, its tag join rows, and its contact join rows are removed; the referenced company, contacts, tags, and users still exist

### Non-admin cannot delete

- **Given** an existing opportunity and a caller without the APP-ADMIN role
- **When** `DELETE /api/opportunities/{id}` is called
- **Then** the response is `403 Forbidden` and the opportunity still exists

## Delete blocking on referenced entities

### Company deletion is blocked

- **Given** a company referenced by at least one opportunity
- **When** `DELETE /api/companies/{id}` is called (as admin)
- **Then** the response is `409 Conflict`, the company and the opportunity still exist

### Company deletion with deleteContacts is blocked by main contacts

- **Given** a company whose contact C is the main contact of an opportunity belonging to a *different* company, and the company itself has no opportunities
- **When** `DELETE /api/companies/{id}?deleteContacts=true` is called (as admin)
- **Then** the response is `409 Conflict` and nothing is deleted

### Company without opportunities can still be deleted

- **Given** a company that no opportunity references (directly or via main contacts when `deleteContacts=true`)
- **When** `DELETE /api/companies/{id}` is called (as admin)
- **Then** the deletion succeeds as before spec 113

### Main contact deletion is blocked

- **Given** a contact that is the main contact of an opportunity
- **When** `DELETE /api/contacts/{id}` is called (as admin)
- **Then** the response is `409 Conflict` and the contact still exists

### Additional contact deletion silently unlinks

- **Given** a contact that is only an *additional* contact of an opportunity
- **When** `DELETE /api/contacts/{id}` is called (as admin)
- **Then** the response is `204 No Content`, the contact is deleted, and the opportunity remains with the contact removed from its additional contacts

## Comments

### Add a comment

- **Given** an existing opportunity and the request is authenticated as user U
- **When** `POST /api/opportunities/{id}/comments` is called with text "Called them today"
- **Then** the response is `201 Created` with a `CommentDto` whose author is user U; the opportunity's `commentCount` increases by 1; an audit log entry with entityType "OpportunityComment" and action INSERT is written

### List comments

- **Given** an opportunity with three comments
- **When** `GET /api/opportunities/{id}/comments` is called
- **Then** the response is `200 OK` with all three comments including nested author `UserDto`s

### Update a comment

- **Given** an opportunity with a comment
- **When** `PUT /api/opportunities/{id}/comments/{commentId}` is called with new text
- **Then** the response is `200 OK`, the text is updated, the original author is preserved, and an "OpportunityComment" UPDATE audit entry is written

### Cross-owner comment access returns 404

- **Given** a comment that belongs to opportunity A
- **When** it is updated or deleted via opportunity B's comment endpoints
- **Then** the response is `404 Not Found`

### Admin deletes a comment

- **Given** an opportunity with a comment and a caller with the APP-ADMIN role
- **When** `DELETE /api/opportunities/{id}/comments/{commentId}` is called
- **Then** the response is `204 No Content`, the comment is removed, `commentCount` decreases, and an "OpportunityComment" DELETE audit entry is written

### Blank comment text is rejected

- **When** a comment is added or updated with blank text
- **Then** the response is `400 Bad Request`

## Tags

### Deleting a tag detaches it from opportunities

- **Given** an opportunity with tag T
- **When** tag T is deleted
- **Then** the opportunity still exists and no longer references T

### Tag counts include opportunities

- **Given** tag T assigned to one company, one contact, and two opportunities
- **When** the tag list endpoint is called
- **Then** tag T reports an opportunity count of 2 alongside the existing company and contact counts

## Search integration

### Created opportunity becomes searchable

- **Given** the search index is bootstrapped
- **When** an opportunity titled "Muster-Bank CRA" is created
- **Then** a search for "muster" returns it in the `opportunities` section of `GET /api/search`

### Updated opportunity is re-indexed

- **Given** an indexed opportunity
- **When** its title changes
- **Then** searching for the new title finds it and the old title no longer matches

### Deleted opportunity disappears from search

- **Given** an indexed opportunity
- **When** it is deleted
- **Then** it no longer appears in search results

### Bootstrap indexes existing opportunities

- **Given** opportunities exist in the database
- **When** the backend starts and the search bootstrap runs
- **Then** all opportunities are indexed in the `opportunities` index

### Opportunity comments are searchable with owner label

- **Given** an opportunity with a comment "budget approved"
- **When** a search for "budget" is executed
- **Then** the comment appears in the `comments` section with the opportunity's title as owner label

## Updates feed

### Opportunity lifecycle appears in the feed

- **Given** user U creates, updates, and deletes an opportunity
- **When** `GET /api/updates` is called
- **Then** the feed contains OPPORTUNITY_CREATED/UPDATED/DELETED entries attributed to user U, showing the opportunity title

### Opportunity comment activity appears in the feed

- **Given** user U adds a comment to an opportunity
- **When** `GET /api/updates` is called
- **Then** the feed contains an OPPORTUNITY_COMMENT_CREATED entry with the opportunity's title

## MCP tools

### list_opportunities returns a page

- **Given** 30 opportunities and an authenticated MCP session
- **When** the `list_opportunities` tool is called with page 0 and size 20
- **Then** it returns 20 items, `totalCount` 30, and `hasMore` true

### list_opportunities filters

- **Given** opportunities with different statuses and companies
- **When** `list_opportunities` is called with a `status` or `companyId` filter
- **Then** only matching opportunities are returned

### get_opportunity by ID

- **Given** an existing opportunity
- **When** the `get_opportunity` tool is called with its ID
- **Then** the full opportunity data is returned

### get_opportunity with unknown ID

- **When** `get_opportunity` is called with a non-existing UUID
- **Then** an MCP not-found tool error is returned

### list_opportunity_comments

- **Given** an opportunity with comments
- **When** the `list_opportunity_comments` tool is called
- **Then** the comments are returned with in-memory pagination

## User options endpoint

### Any authenticated user can list user options

- **Given** three registered users plus the SYSTEM-USER, and a caller without any admin role
- **When** `GET /api/users/options` is called
- **Then** the response is `200 OK` with three entries containing only `id`, `name`, and `avatarUrl` (no email); the SYSTEM-USER is excluded

### Unauthenticated access is rejected

- **When** `GET /api/users/options` is called without a token
- **Then** the response is `401 Unauthorized`

## Audit log

### Opportunity mutations are audited

- **Given** user U creates and then updates an opportunity
- **When** the audit log is inspected
- **Then** it contains an INSERT and an UPDATE entry with entityType "OpportunityDto", the opportunity's ID, its title as name, and user U
