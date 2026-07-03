// Prebuild step (wired into `pnpm build`): generates the two PWA files that must
// exist in public/ before `next build` (output: standalone copies public/).
//
//   public/offline.html  — self-contained bilingual offline page (branding applied)
//   public/sw.js         — the minimal service worker, cache-versioned by the
//                          content hash of offline.html
//
// The generic generators live in src/lib/pwa/*.ts and are imported directly:
// Node (>= 24, matching the Docker base) strips the TypeScript types at runtime,
// so no transpile step or extra dependency is required. Icons are committed
// separately (scripts/generate-icons.mjs) and are not touched here.

import { createHash } from "node:crypto";
import { writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { buildMarkSvg, BRAND_GREEN } from "./brand-mark.mjs";
import { buildOfflineHtml } from "../src/lib/pwa/build-offline-html.ts";
import { renderServiceWorker } from "../src/lib/pwa/service-worker-source.ts";

const here = dirname(fileURLToPath(import.meta.url));
const publicDir = join(here, "..", "public");
const logoPath = join(publicDir, "oe-logo-landscape-dark.svg");

// The offline page sits on a white background, so the logo is shown inside the
// rounded green brand tile (white wordmark on green) to stay visible and on-brand.
const logoSvg = buildMarkSvg({ logoPath, size: 256, widthFraction: 0.74, radius: 32 });

const offlineHtml = buildOfflineHtml({
  appName: "Open CRM",
  primaryColor: BRAND_GREEN,
  backgroundColor: "#ffffff",
  logoSvg,
});
writeFileSync(join(publicDir, "offline.html"), offlineHtml);

const cacheVersion = createHash("sha256").update(offlineHtml).digest("hex").slice(0, 12);
const swSource = renderServiceWorker({ cacheVersion });
writeFileSync(join(publicDir, "sw.js"), swSource);

console.log(`Generated public/offline.html and public/sw.js (cache version ${cacheVersion})`);
