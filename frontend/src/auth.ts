import NextAuth from "next-auth";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    idToken?: string;
  }
}

export const oidcIssuer = process.env.OIDC_ISSUER_URI;

export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [
    {
      id: "oidc",
      name: "OIDC",
      type: "oidc",
      issuer: oidcIssuer,
      clientId: process.env.OIDC_CLIENT_ID,
      clientSecret: process.env.OIDC_CLIENT_SECRET,
      authorization: { params: { scope: "openid profile email offline_access" } },
    },
  ],
  pages: { signIn: "/login" },
  session: { strategy: "jwt" },
  callbacks: {
    authorized({ auth: session }) {
      // Returning false triggers a redirect to the sign-in page
      return !!session?.user;
    },
    async signIn() {
      return true;
    },
    async jwt({ token, account, profile }) {
      const t = token as Record<string, unknown>;

      // On initial sign-in, store tokens from the OIDC provider
      if (account) {
        t.accessToken = account.access_token;
        t.refreshToken = account.refresh_token;
        t.idToken = account.id_token;
        t.expiresAt = account.expires_at;
        if (profile) {
          t.name = profile.name;
          t.email = profile.email;
          t.picture = profile.picture;
        }
        return token;
      }

      // Return token if not expired
      if (
        typeof t.expiresAt === "number" &&
        Date.now() < t.expiresAt * 1000
      ) {
        return token;
      }

      // Attempt to refresh the access token
      if (typeof t.refreshToken === "string") {
        try {
          const issuer = process.env.OIDC_ISSUER_URI;
          const wellKnownResponse = await fetch(
            `${issuer}/.well-known/openid-configuration`,
          );
          const wellKnown = await wellKnownResponse.json();
          const tokenEndpoint = wellKnown.token_endpoint;

          const response = await fetch(tokenEndpoint, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({
              grant_type: "refresh_token",
              client_id: process.env.OIDC_CLIENT_ID!,
              client_secret: process.env.OIDC_CLIENT_SECRET!,
              refresh_token: t.refreshToken as string,
            }),
          });

          const refreshed = await response.json();

          if (!response.ok) {
            throw new Error("Token refresh failed");
          }

          t.accessToken = refreshed.access_token;
          t.refreshToken = refreshed.refresh_token ?? t.refreshToken;
          t.expiresAt = Math.floor(
            Date.now() / 1000 + refreshed.expires_in,
          );
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
