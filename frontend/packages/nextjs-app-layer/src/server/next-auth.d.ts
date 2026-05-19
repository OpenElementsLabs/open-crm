import "next-auth";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    idToken?: string;
    expiresAt?: number;
    roles: string[];
    error?: string;
  }
}
