import { cache } from "react";
import { getCompany, getContact } from "@/lib/api";

/**
 * Request-scoped cached wrappers around {@link getContact} / {@link getCompany}.
 *
 * A detail route resolves its entity twice per render pass — once in `generateMetadata` and once in
 * the page component — and, because `apiFetch` attaches a per-request `Authorization` header and the
 * routes are `force-dynamic`, Next.js' built-in fetch deduplication does not collapse the two calls.
 * Wrapping in React's {@link cache} makes both callers (and the page's own `notFound()` path) share
 * a single in-flight backend request keyed by the entity id.
 *
 * These wrappers live in their own module — rather than replacing the exports in `@/lib/api` — because
 * `cache` is a Server-Component API, while `getContact` is also called from the client
 * (`opportunity-detail.tsx`). Only server components and the metadata/image routes import this file.
 */
export const getCachedContact = cache(getContact);

export const getCachedCompany = cache(getCompany);
