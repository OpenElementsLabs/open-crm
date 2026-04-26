# Implementation Steps: Admin Users View

## Overview

Read-only admin page listing all registered users. Backend: paginated `GET /api/users` (IT-ADMIN only). Frontend: server page + paginated client table behind admin sub-menu nav item.

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| IT-ADMIN can access the users page | Frontend | 4, 8 |
| Non-IT-ADMIN is shown forbidden page | Frontend | 4, 8 |
| Unauthenticated user cannot access the endpoint | Backend | 1, 6 |
| Non-IT-ADMIN cannot access the endpoint | Backend | 1, 6 |
| Users item visible for IT-ADMIN | Frontend | 5, 8 |
| Users item hidden for non-IT-ADMIN | Frontend | 5, 8 |
| Display users with all fields | Frontend | 4, 8 |
| Display user with null avatarUrl | Frontend | 4, 8 |
| Avatar is displayed as circular thumbnail | Frontend | 4, 8 |
| Loading state shows skeletons | Frontend | 4, 8 |
| Empty state when no users exist | Frontend | 4, 8 |
| Default page size is 20 | Frontend | 4, 8 |
| Page size is persisted in localStorage | Frontend | 4, 8 |
| Page size options match other tables | Frontend | 4, 8 |
| Total user count is displayed | Frontend | 4, 8 |
| Pagination buttons shown when needed | Frontend | 4, 8 |
| Pagination buttons hidden for single page | Frontend | 4, 8 |
| Previous button disabled on first page | Frontend | 4, 8 |
| Next button disabled on last page | Frontend | 4, 8 |
| Navigate to next page | Frontend | 4, 8 |
| Navigate to previous page | Frontend | 4, 8 |
| Changing page size resets to first page | Frontend | 4, 8 |
| Paginated response structure | Backend | 1, 6 |
| Default pagination without parameters | Backend | 1, 6 |

---

## Step 1 — Backend: paginated GET /api/users endpoint

**Changes**

- [x] Add `@GetMapping` method `listUsers(Pageable)` to `backend/src/main/java/com/openelements/crm/user/UserController.java`
- [x] Annotate with `@RequiresItAdmin`
- [x] Add OpenAPI `@Operation` and `@Parameter` annotations for `page` and `size`
- [x] Use `@PageableDefault(size = 20)` so unspecified params default to size 20, page 0
- [x] Return `Page<UserDto>` from `userService.findAll(pageable)`

**Acceptance criteria**

- [x] Backend builds: `cd backend && ./mvnw compile`
- [ ] Endpoint returns 200 + `Page<UserDto>` JSON when called as IT-ADMIN
- [ ] Endpoint returns 401 unauthenticated, 403 for other roles

**Related behaviors:** Unauthenticated user cannot access the endpoint; Non-IT-ADMIN cannot access the endpoint; Paginated response structure; Default pagination without parameters

---

## Step 2 — Frontend: API function `getUsers`

**Changes**

- [ ] Add `UserListParams` interface and `getUsers(params)` function to `frontend/src/lib/api.ts`
- [ ] Follow the pattern of `getContacts`: `URLSearchParams` for `page`/`size`, `apiFetch` with `cache: "no-store"`, return `Page<UserDto>`
- [ ] Reuse existing `UserDto` type from `frontend/src/lib/types.ts`

**Acceptance criteria**

- [ ] Frontend type-checks: `pnpm --filter frontend exec tsc --noEmit`

---

## Step 3 — Frontend: i18n keys

**Changes**

- [ ] Add `nav.users` key to `frontend/src/lib/i18n/en.ts` (`"Users"`) and `de.ts` (`"Benutzer"`)
- [ ] Add new `users` namespace with: `title`, `empty`, `columns.{avatar,name,email}`, `pagination.{perPage,previous,next,totalOne,totalOther}` to both files
- [ ] Use Brevo/webhook namespaces as structural template

**Acceptance criteria**

- [ ] Frontend type-checks (translation typing across en/de matches)

---

## Step 4 — Frontend: page + client component

**Changes**

- [ ] Create `frontend/src/app/(app)/admin/users/page.tsx` (server component)
  - [ ] Call `auth()`, check `ROLE_IT_ADMIN`, return `<ForbiddenPage />` if missing
  - [ ] Render `<UsersClient />` inside heading layout matching `admin/status/page.tsx`
- [ ] Create `frontend/src/app/(app)/admin/users/users-client.tsx`
  - [ ] State: `data: Page<UserDto> | null`, `pageSize` (localStorage `pageSize.users`, default 20), `page` (0-based), `loading`
  - [ ] Effect: load `getUsers({ page, size: pageSize })` on mount + when page/size change
  - [ ] Render Table with columns: Avatar (32x32 circular `<img>` from `avatarUrl`, fallback `<User>` icon in neutral circle), Name, Email
  - [ ] Skeleton rows during loading; empty state with `<Users>` icon + `t.users.empty` when `totalElements === 0`
  - [ ] Page-size `<Select>` with options 10/20/50/100/200; on change persist to localStorage and reset `page` to 0
  - [ ] Total-count text using `t.users.pagination.totalOne` / `totalOther`
  - [ ] Prev/Next buttons hidden when `totalPages <= 1`; disabled at boundaries
- [ ] Imports from `@open-elements/ui`: `Button`, `Select*`, `Table*`, `Skeleton`

**Acceptance criteria**

- [ ] `pnpm --filter frontend dev` runs and `/admin/users` renders for IT-ADMIN
- [ ] Non-IT-ADMIN sees ForbiddenPage
- [ ] Frontend type-checks

**Related behaviors:** all UI rows in the coverage table

---

## Step 5 — Frontend: sidebar nav entry

**Changes**

- [ ] Edit `frontend/src/app/(app)/layout.tsx`: add a `<NavItem>` for `/admin/users` inside the existing admin `<CollapsibleGroup>`, after the Webhooks item
- [ ] Use `Users` icon from `lucide-react`, label `t.nav.users`
- [ ] Visibility inherited from existing `canSeeAdmin` gate — no extra logic needed

**Acceptance criteria**

- [ ] Sidebar shows "Users" item under Admin for IT-ADMIN
- [ ] Sidebar admin section is hidden for non-IT-ADMIN (existing behavior, no regression)

**Related behaviors:** Users item visible for IT-ADMIN; Users item hidden for non-IT-ADMIN

---

## Step 6 — Backend tests

**Changes**

- [ ] Create `backend/src/test/java/com/openelements/crm/user/UserControllerTest.java`
- [ ] Use `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` and the `withRoles(...)` JWT helper pattern from `SecurityRoleIntegrationTest`
- [ ] Tests:
  - [ ] `GET /api/users` without auth → 401
  - [ ] `GET /api/users` as USER → 403
  - [ ] `GET /api/users` as ADMIN → 403 (only IT-ADMIN allowed)
  - [ ] `GET /api/users` as IT-ADMIN → 200, body has `content[]`, `page.size`, `page.number`, `page.totalElements`, `page.totalPages`
  - [ ] `GET /api/users?page=0&size=10` as IT-ADMIN → 200, `page.size==10`
  - [ ] `GET /api/users` (no params) as IT-ADMIN → 200, `page.size==20` (default)

**Acceptance criteria**

- [ ] `cd backend && ./mvnw test` passes

**Related behaviors:** Unauthenticated user cannot access the endpoint; Non-IT-ADMIN cannot access the endpoint; Paginated response structure; Default pagination without parameters

---

## Step 7 — (intentionally empty)

Reserved to keep step numbering aligned with other specs in this repo. Skip.

---

## Step 8 — Frontend tests

**Changes**

- [ ] Create `frontend/src/app/(app)/admin/users/__tests__/users-client.test.tsx` (Vitest + React Testing Library)
- [ ] Mock `@/lib/api` `getUsers`, `next/navigation`, and `localStorage` per the `api-key-list.test.tsx` pattern
- [ ] Tests:
  - [ ] Renders skeleton during loading
  - [ ] Renders empty state when totalElements === 0
  - [ ] Renders rows with avatar `<img>` when `avatarUrl` present
  - [ ] Renders fallback icon when `avatarUrl` is null
  - [ ] Default page size is 20 on first load (no localStorage)
  - [ ] Selected page size is persisted to `localStorage["pageSize.users"]`
  - [ ] Page-size selector shows options 10/20/50/100/200
  - [ ] Total user count text is rendered
  - [ ] Prev/Next hidden when `totalPages === 1`
  - [ ] Prev disabled on first page; Next disabled on last
  - [ ] Click Next requests page 1; Click Prev requests page 0
  - [ ] Changing page size resets page to 0
- [ ] Add a small page-level test (or extend existing layout test if present) verifying:
  - [ ] `page.tsx` returns `<ForbiddenPage />` when session has no IT-ADMIN role
  - [ ] Sidebar `Users` nav item exists for IT-ADMIN session (covered indirectly by the existing `canSeeAdmin` gate; if a layout test exists, extend it; otherwise note as covered by Step 4 page test)

**Acceptance criteria**

- [ ] `pnpm --filter frontend test` passes

**Related behaviors:** all UI rows in the coverage table

---

## Step 9 — Update project documentation

**Changes**

- [ ] Update `.claude/conventions/project-specific/project-features.md` with the new admin users view feature (if file exists and is non-empty)
- [ ] Update `specs/INDEX.md`: status `089` → `done` (done as part of spec-flow Step 6, not here)

**Acceptance criteria**

- [ ] Docs reference the new page accurately
