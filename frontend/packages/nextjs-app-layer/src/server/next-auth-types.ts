// Type augmentation for next-auth's Session.
// Apps activate this via a side-effect import in their own auth.ts:
//   import "@open-elements/nextjs-app-layer/server/next-auth-types";
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

export {};
