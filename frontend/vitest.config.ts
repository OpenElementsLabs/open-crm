import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "path";

// `@open-elements/nextjs-app-layer` ships both `dist/` (built) and `src/`
// (raw TS) under its npm tarball. Open CRM consumes it in source mode at
// build time via Next.js' `transpilePackages`; mirror that here so Vitest
// uses the same source files. The published `dist/*.js` uses extension-less
// relative imports that Node's ESM resolver rejects, so without these
// aliases Vitest fails immediately on the lib's barrel.
const appLayer = path.resolve(
  __dirname,
  "node_modules/@open-elements/nextjs-app-layer",
);

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
  },
  resolve: {
    alias: {
      "@open-elements/nextjs-app-layer/layout": `${appLayer}/src/layout/root-layout.tsx`,
      "@open-elements/nextjs-app-layer/server/next-auth-types": `${appLayer}/src/server/next-auth-types.ts`,
      "@open-elements/nextjs-app-layer/server": `${appLayer}/src/server.ts`,
      "@open-elements/nextjs-app-layer": `${appLayer}/src/index.ts`,
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
