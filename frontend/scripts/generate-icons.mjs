// One-off generator for the committed PWA icons.
//
// Builds a square brand mark — the Open Elements landscape wordmark rendered
// monochrome white, centered on the green brand background (--color-oe-green) —
// and rasterises it to the icon sizes the manifest and <head> reference.
//
// The output PNGs are COMMITTED static assets (see docs/specs/111-pwa-support);
// this script is not part of the build. It uses only macOS built-ins
// (`qlmanage` for SVG rasterisation, `sips` for exact sizing) so it needs no
// extra dependency — `sharp` is intentionally disabled in pnpm-workspace.yaml.
//
// Run from the frontend/ directory:  node scripts/generate-icons.mjs

import { execFileSync } from "node:child_process";
import { mkdirSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { buildMarkSvg } from "./brand-mark.mjs";

const LOGO = "public/oe-logo-landscape-dark.svg";

const work = join(tmpdir(), "oe-pwa-icons");
mkdirSync(work, { recursive: true });
mkdirSync("public/icons", { recursive: true });

const standard = join(work, "standard.svg");
const maskable = join(work, "maskable.svg");
writeFileSync(standard, buildMarkSvg({ logoPath: LOGO, widthFraction: 0.82 })); // fills the tile
writeFileSync(maskable, buildMarkSvg({ logoPath: LOGO, widthFraction: 0.6 })); // maskable safe zone

/** Rasterises `svg` to exactly `size`×`size` at `out`. */
function raster(svg, size, out) {
  execFileSync("qlmanage", ["-t", "-s", String(size), "-o", work, svg], { stdio: "ignore" });
  // qlmanage fits within `size`; sips guarantees an exact square canvas.
  execFileSync("sips", ["-z", String(size), String(size), `${svg}.png`, "--out", out], { stdio: "ignore" });
}

raster(standard, 512, "public/icons/icon-512.png");
raster(standard, 192, "public/icons/icon-192.png");
raster(maskable, 512, "public/icons/icon-maskable-512.png");
raster(standard, 180, "public/apple-touch-icon.png");
raster(standard, 32, "public/favicon-32.png");
raster(standard, 16, "public/favicon-16.png");

rmSync(work, { recursive: true, force: true });
console.log("Generated PWA icons in public/ and public/icons/");
