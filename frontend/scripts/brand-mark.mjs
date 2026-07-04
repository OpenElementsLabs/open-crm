// Shared helper: composes the square Open Elements brand mark — the landscape
// wordmark rendered monochrome white, centered on the green brand background.
// Used by the one-off icon generator and by the build-time offline-page logo.

import { readFileSync } from "node:fs";

export const BRAND_GREEN = "#5cba9e";
const LOGO_W = 653.15;
const LOGO_H = 85.73;

/** Extracts the logo's drawable content and drops its CSS classes so it inherits a single fill. */
function logoInner(logoPath) {
  const raw = readFileSync(logoPath, "utf8");
  const inner = raw.replace(/<\?xml[^>]*\?>/, "").match(/<svg[^>]*>([\s\S]*)<\/svg>/)[1];
  return inner.replace(/<defs>[\s\S]*?<\/defs>/g, "").replace(/\sclass="[^"]*"/g, "");
}

/**
 * Builds a square brand-mark SVG string.
 *
 * @param {object} opts
 * @param {string} opts.logoPath   path to the landscape logo SVG
 * @param {number} [opts.size]     canvas size (default 512)
 * @param {number} [opts.widthFraction] wordmark width as a fraction of the canvas (default 0.82)
 * @param {number} [opts.radius]   corner radius (default 0 = square tile)
 * @returns {string} the composed SVG markup
 */
export function buildMarkSvg({ logoPath, size = 512, widthFraction = 0.82, radius = 0 }) {
  const inner = logoInner(logoPath);
  const width = size * widthFraction;
  const scale = width / LOGO_W;
  const height = LOGO_H * scale;
  const tx = (size - width) / 2;
  const ty = (size - height) / 2;
  const rect = `<rect width="${size}" height="${size}" rx="${radius}" ry="${radius}" fill="${BRAND_GREEN}"/>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${size} ${size}" width="${size}" height="${size}">
  ${rect}
  <g fill="#ffffff" transform="translate(${tx.toFixed(2)} ${ty.toFixed(2)}) scale(${scale.toFixed(5)})">${inner}</g>
</svg>`;
}
