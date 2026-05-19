import { NextRequest, NextResponse } from "next/server";
import type { Session } from "next-auth";

type AuthFn = () => Promise<Session | null>;

type RouteHandler = (
  req: NextRequest,
  context: { params: Promise<{ path: string[] }> },
) => Promise<Response>;

export interface BackendProxyConfig {
  readonly backendUrl: string;
  readonly auth: AuthFn;
}

/**
 * Create a route handler that proxies an authenticated request to the
 * upstream backend, attaching `Authorization: Bearer <accessToken>` from
 * the session cookie and forwarding query params / body / Content-Type /
 * Accept headers.
 *
 * Mount as `export { handler as GET, handler as POST, handler as PUT, handler as DELETE }`
 * in `frontend/src/app/api/[...path]/route.ts`.
 */
export function createBackendProxyHandler(config: BackendProxyConfig): RouteHandler {
  const { backendUrl, auth } = config;
  return async function handler(req, { params }) {
    const session = await auth();
    const { path } = await params;
    const target = `${backendUrl}/api/${path.join("/")}`;

    const url = new URL(target);
    const reqUrl = new URL(req.url);
    reqUrl.searchParams.forEach((value, key) => {
      url.searchParams.append(key, value);
    });

    const headers = new Headers();
    const contentType = req.headers.get("Content-Type");
    if (contentType) headers.set("Content-Type", contentType);
    const accept = req.headers.get("Accept");
    if (accept) headers.set("Accept", accept);
    if (session?.accessToken) {
      headers.set("Authorization", `Bearer ${session.accessToken}`);
    }

    const hasBody = req.method !== "GET" && req.method !== "HEAD";

    const response = await fetch(url.toString(), {
      method: req.method,
      headers,
      body: hasBody ? await req.arrayBuffer() : undefined,
    });

    return new Response(response.body, {
      status: response.status,
      headers: response.headers,
    });
  };
}

export interface LogoutHandlerConfig {
  readonly auth: AuthFn;
  readonly oidcIssuer: string | undefined;
  readonly authUrl: string;
}

const SESSION_COOKIE_PREFIXES = [
  "authjs.session-token",
  "__Secure-authjs.session-token",
];

/**
 * Create a route handler that performs an OIDC end-session flow and
 * deletes all Auth.js session cookies (including chunked variants).
 *
 * Mount as `export { handler as GET }` in `frontend/src/app/api/logout/route.ts`.
 */
export function createLogoutHandler(config: LogoutHandlerConfig) {
  const { auth, oidcIssuer, authUrl } = config;
  return async function handler(req: NextRequest): Promise<Response> {
    const session = await auth();
    const idToken = session?.idToken;

    const loginUrl = `${authUrl}/login`;
    let endSessionUrl = loginUrl;
    if (oidcIssuer) {
      try {
        const wellKnownResponse = await fetch(
          `${oidcIssuer}/.well-known/openid-configuration`,
        );
        const wellKnown = await wellKnownResponse.json();
        const endSessionEndpoint = wellKnown.end_session_endpoint;
        if (endSessionEndpoint) {
          const params = new URLSearchParams();
          if (idToken) params.set("id_token_hint", idToken);
          params.set("post_logout_redirect_uri", loginUrl);
          endSessionUrl = `${endSessionEndpoint}?${params.toString()}`;
        }
      } catch {
        // Fall back to /login if discovery fails
      }
    }

    const response = NextResponse.redirect(endSessionUrl);

    const isSecure = authUrl.startsWith("https://");
    const cookieOptions = {
      path: "/",
      secure: isSecure,
      httpOnly: true,
      sameSite: "lax" as const,
    };

    for (const prefix of SESSION_COOKIE_PREFIXES) {
      response.cookies.delete({ name: prefix, ...cookieOptions });
      for (const cookie of req.cookies.getAll()) {
        if (cookie.name.startsWith(`${prefix}.`)) {
          response.cookies.delete({ name: cookie.name, ...cookieOptions });
        }
      }
    }

    return response;
  };
}
