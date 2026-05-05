import { NextRequest, NextResponse } from "next/server";
import { auth, oidcIssuer } from "@/auth";

const SESSION_COOKIE_PREFIXES = [
  "authjs.session-token",
  "__Secure-authjs.session-token",
];

export async function GET(req: NextRequest) {
  const session = await auth();
  const idToken = session?.idToken;

  // Discover the OIDC provider's end-session endpoint
  const baseUrl = process.env.AUTH_URL ?? "http://localhost:3000";
  const loginUrl = `${baseUrl}/login`;
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
        if (idToken) {
          params.set("id_token_hint", idToken);
        }
        params.set("post_logout_redirect_uri", loginUrl);
        endSessionUrl = `${endSessionEndpoint}?${params.toString()}`;
      }
    } catch {
      // Fall back to /login if discovery fails
    }
  }

  const response = NextResponse.redirect(endSessionUrl);

  // Delete all Auth.js session cookies, including chunked cookies (.0, .1, .2, ...)
  // Auth.js splits large JWT sessions across multiple cookies when they exceed 4KB
  // Cookie attributes must match the original: secure=true only under HTTPS
  const isSecure = baseUrl.startsWith("https://");
  const cookieOptions = {
    path: "/",
    secure: isSecure,
    httpOnly: true,
    sameSite: "lax" as const,
  };

  for (const prefix of SESSION_COOKIE_PREFIXES) {
    // Delete the base cookie
    response.cookies.delete({ name: prefix, ...cookieOptions });

    // Delete any chunked cookies found in the request
    for (const cookie of req.cookies.getAll()) {
      if (cookie.name.startsWith(`${prefix}.`)) {
        response.cookies.delete({ name: cookie.name, ...cookieOptions });
      }
    }
  }

  return response;
}
