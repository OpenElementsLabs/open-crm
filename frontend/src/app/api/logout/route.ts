import { NextResponse } from "next/server";
import { auth, oidcIssuer } from "@/auth";

export async function GET() {
  const session = await auth();
  const idToken = session?.idToken;

  // Discover the OIDC provider's end-session endpoint
  const baseUrl = process.env.AUTH_URL ?? "http://localhost:3000";
  let endSessionUrl = baseUrl;
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
        params.set("post_logout_redirect_uri", process.env.AUTH_URL ?? "http://localhost:3000");
        endSessionUrl = `${endSessionEndpoint}?${params.toString()}`;
      }
    } catch {
      // Fall back to app root if discovery fails
    }
  }

  // Clear the Auth.js session cookie by redirecting through the signout endpoint
  // Then redirect to the provider's end-session endpoint
  const response = NextResponse.redirect(endSessionUrl);
  // Delete the Auth.js session cookie
  response.cookies.delete("authjs.session-token");
  response.cookies.delete("__Secure-authjs.session-token");
  return response;
}
