import NextAuth from "next-auth";

export interface AppLayerAuthConfig {
  readonly issuer: string | undefined;
  readonly clientId: string | undefined;
  readonly clientSecret: string | undefined;
}

/**
 * Create the OE-standard NextAuth configuration with OIDC provider,
 * JWT strategy, refresh-token flow, and session-claim mapping.
 *
 * Returns the four NextAuth pieces plus the resolved `oidcIssuer` (some
 * callers like the logout handler need it).
 */
export function createAppLayerAuth(config: AppLayerAuthConfig) {
  const { issuer: oidcIssuer, clientId, clientSecret } = config;

  const nextAuth = NextAuth({
    providers: [
      {
        id: "oidc",
        name: "OIDC",
        type: "oidc",
        issuer: oidcIssuer,
        clientId,
        clientSecret,
        authorization: { params: { scope: "openid profile email offline_access roles" } },
      },
    ],
    pages: { signIn: "/login" },
    session: { strategy: "jwt" },
    callbacks: {
      authorized({ auth: session }) {
        return !!session?.user;
      },
      async signIn() {
        return true;
      },
      async jwt({ token, account, profile }) {
        const t = token as Record<string, unknown>;

        if (account) {
          t.accessToken = account.access_token;
          t.refreshToken = account.refresh_token;
          t.idToken = account.id_token;
          t.expiresAt = account.expires_at;
          if (profile) {
            t.name = profile.name;
            t.email = profile.email;
            t.picture = profile.picture;
            const profileRoles = (profile as Record<string, unknown>).roles;
            t.roles = Array.isArray(profileRoles) ? profileRoles : [];
          }
          return token;
        }

        if (
          typeof t.expiresAt === "number" &&
          Date.now() < (t.expiresAt - 60) * 1000
        ) {
          return token;
        }

        if (typeof t.refreshToken === "string") {
          try {
            const wellKnownResponse = await fetch(
              `${oidcIssuer}/.well-known/openid-configuration`,
            );
            const wellKnown = await wellKnownResponse.json();
            const tokenEndpoint = wellKnown.token_endpoint;

            const response = await fetch(tokenEndpoint, {
              method: "POST",
              headers: { "Content-Type": "application/x-www-form-urlencoded" },
              body: new URLSearchParams({
                grant_type: "refresh_token",
                client_id: clientId!,
                client_secret: clientSecret!,
                refresh_token: t.refreshToken as string,
              }),
            });

            const refreshed = await response.json();

            if (!response.ok) {
              throw new Error("Token refresh failed");
            }

            t.accessToken = refreshed.access_token;
            t.refreshToken = refreshed.refresh_token ?? t.refreshToken;
            t.expiresAt = Math.floor(Date.now() / 1000 + refreshed.expires_in);
            t.error = undefined;
            return token;
          } catch (error) {
            console.error("Token refresh failed:", error);
            t.error = "RefreshTokenError";
            return token;
          }
        }

        return token;
      },
      async session({ session, token }) {
        const t = token as Record<string, unknown>;
        session.accessToken = t.accessToken as string | undefined;
        session.idToken = t.idToken as string | undefined;
        session.expiresAt = t.expiresAt as number | undefined;
        session.roles = Array.isArray(t.roles) ? (t.roles as string[]) : [];
        session.error = typeof t.error === "string" ? t.error : undefined;
        if (t.error === "RefreshTokenError") {
          session.accessToken = undefined;
        }
        if (typeof t.name === "string") session.user.name = t.name;
        if (typeof t.email === "string") session.user.email = t.email;
        if (typeof t.picture === "string") session.user.image = t.picture;
        return session;
      },
    },
  });

  return { ...nextAuth, oidcIssuer };
}
