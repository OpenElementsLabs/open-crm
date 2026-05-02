# Frontend Structure and Conventions

Conventions for the Open CRM frontend (`frontend/`). For setup and run commands see the root `README.md`. For library-link workflows see `development.md`.

## Tech stack

| Layer | Choice |
|---|---|
| Framework | Next.js 15 (App Router, React 19, TypeScript) |
| UI library | `@open-elements/ui` (in-house design system) |
| Styling | Tailwind CSS |
| Auth | NextAuth (OIDC against Authentik) |
| API client | hand-written, typed, in `src/lib/api.ts` |
| i18n | `src/lib/i18n/{de,en}.ts` + `useTranslations()` |
| Testing | Vitest + Testing Library, jsdom environment |
| Package manager | pnpm |

## Directory layout

```
frontend/src/
  app/                 ← Next.js App Router
    (app)/             ← route group: authenticated app shell
      layout.tsx       ← shared layout (sidebar, header, providers)
      page.tsx         ← dashboard
      contacts/        ← one folder per top-level resource
        page.tsx       ← Server Component (thin wrapper)
        contacts-client.tsx          ← Client Component with all logic
        __tests__/contacts-client.test.tsx
        new/page.tsx
        [id]/page.tsx
        [id]/edit/page.tsx
        print/page.tsx
      companies/, tags/, api-keys/, webhooks/   ← same shape
      admin/
        users/, audit-logs/, token/, status/, brevo/
    api/               ← Next.js Route Handlers (auth, proxy)
    login/page.tsx
  components/          ← SHARED components (≥ 2 consumers)
  lib/                 ← api, i18n, roles, types, utils
  test/                ← test helpers (renderWithProviders, setup)
```

Path alias: `@/*` → `./src/*` (`tsconfig.json`).

## Page = Server Component, `*-client.tsx` = Client Component

Every route follows the same split:

- `page.tsx` — Server Component. Stays thin: only auth/role gating and rendering the client. Marked `export const dynamic = "force-dynamic"` when it must run per-request.
- `<route-name>-client.tsx` — Client Component (`"use client"`). All hooks, state, effects, fetches.
- `__tests__/<route-name>-client.test.tsx` — co-located test.

Example (`src/app/(app)/admin/audit-logs/page.tsx`):

```tsx
import { auth } from "@/auth";
import { ForbiddenPage } from "@/components/forbidden-page";
import { ROLE_IT_ADMIN } from "@/lib/roles";
import { AuditLogsClient } from "./audit-logs-client";

export default async function AuditLogsPage() {
  const session = await auth();
  if (!session?.roles?.includes(ROLE_IT_ADMIN)) return <ForbiddenPage />;
  return <AuditLogsClient />;
}
```

This split exists because Server Components cannot use hooks or effects, and Client Components cannot use `auth()`. Keeping the page thin also makes the client unit-testable in isolation.

## Co-location rule for components

| Used by | Lives in |
|---|---|
| Exactly one route | Co-located next to that route's `page.tsx` (e.g. `src/app/(app)/contacts/contacts-client.tsx`) |
| Two or more routes, or a generic primitive | `src/components/` |

A component starts co-located. Promote to `src/components/` only when a second route imports it.

Examples already in `src/components/`: `add-comment-dialog`, `csv-export-dialog`, `forbidden-page`, `company-form`, `contact-form`, `session-provider`, `translate-dialog`. All are used by multiple routes or are generic wrappers.

## Naming

| Item | Convention | Example |
|---|---|---|
| File name | kebab-case | `audit-logs-client.tsx` |
| Component export | PascalCase, suffix `Client` for the route's main client component | `AuditLogsClient` |
| Test file | mirrors source name + `.test.tsx`, in sibling `__tests__/` | `audit-logs-client.test.tsx` |
| URL slug | kebab-case plural for list routes | `/api-keys`, `/audit-logs` |
| API function | verb + entity in `src/lib/api.ts` | `getAuditLogs`, `createApiKey` |
| Role constant | `ROLE_*` in `src/lib/roles.ts` | `ROLE_IT_ADMIN`, `ROLE_ADMIN` |

## Tests

- Vitest config (`vitest.config.ts`) picks up `src/**/*.test.{ts,tsx}` — anything outside `src/` is invisible to the runner.
- Tests live in a `__tests__/` folder next to the source they test.
- Render with `renderWithProviders` from `src/test/test-utils.tsx` when the component needs `LanguageProvider`, `TooltipProvider`, or `SessionProvider` — most client components do.
- Mock `@/lib/api` with `vi.mock("@/lib/api", () => ({ ... }))`. Mock `next/navigation` (`useRouter`, `usePathname`, `useSearchParams`) when the component reads from them.
- Run: `pnpm test` (vitest) and `pnpm exec tsc --noEmit` for type-checking.

## Recurring patterns inside client components

Spec-driven list views (see `audit-logs-client.tsx`, `users-client.tsx`) export the following constants so tests and other code can reference them rather than hard-coding magic numbers:

```ts
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100, 200] as const;
export const DEFAULT_PAGE_SIZE = 20;
export const PAGE_SIZE_STORAGE_KEY = "pageSize.<resource>";  // e.g. pageSize.auditLogs
```

The `pageSize.<resource>` localStorage key is the canonical place to persist a user's page-size choice. Resource segment is camelCase (`auditLogs`, `apiKeys`).

Other shared conventions:

- Loading states render a Skeleton with `data-testid="<resource>-loading"`.
- Empty states render with `data-testid="<resource>-empty"`.
- Error states render with `data-testid="<resource>-error"`.
- Auth/role gating happens in `page.tsx`, not in the client.

## When adding a new view

1. Create `src/app/(app)/<segment>/page.tsx` (or under `admin/` if admin-only).
2. Add role gating in `page.tsx` if needed (`auth()` + `ForbiddenPage`).
3. Create `<segment>-client.tsx` next to it with `"use client"` and all logic.
4. Add `__tests__/<segment>-client.test.tsx`.
5. Add API functions to `src/lib/api.ts` and types to `src/lib/types.ts`.
6. Add German + English strings to `src/lib/i18n/{de,en}.ts`.
