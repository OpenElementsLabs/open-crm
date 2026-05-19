/**
 * Shared Next.js middleware matcher. Excludes the auth/logout API routes,
 * the public login page, Next.js internal paths, and common static assets.
 *
 * The consuming app keeps a tiny `frontend/src/middleware.ts` that re-exports
 * `auth as middleware` from its own auth.ts and `config` from this module.
 */
export const middlewareConfig = {
  matcher: ["/((?!api/auth|api/logout|login|_next/static|_next/image|favicon\\.ico|.*\\.svg$|.*\\.png$|.*\\.jpg$|.*\\.ico$).*)"],
};
