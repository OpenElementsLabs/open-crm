import type { MetadataRoute } from "next";

/**
 * GENERIC — extraction target for `@open-elements/nextjs-app-layer`.
 *
 * Builds a Web App Manifest from app-provided branding. The app calls this from its
 * `app/manifest.ts`. Kept dependency-free (only a type-only import) so it can be lifted into the
 * shared library unchanged. See `docs/specs/111-pwa-support/design.md` → "Generic vs. app-specific
 * boundary".
 */
export interface CreateManifestOptions {
  /** Full application name shown on the install prompt / splash screen. */
  readonly name: string;
  /** Short name shown under the homescreen icon. */
  readonly shortName: string;
  /** One-line description. */
  readonly description: string;
  /** Manifest theme color (also used by the browser UI). */
  readonly themeColor: string;
  /** Splash-screen background color. */
  readonly backgroundColor: string;
  /** Icon set (already resolved to public URLs). */
  readonly icons: MetadataRoute.Manifest["icons"];
  /** Launch URL; defaults to "/". */
  readonly startUrl?: string;
  /** Navigation scope; defaults to "/". */
  readonly scope?: string;
}

/**
 * Produces a standalone-display manifest. Metadata is single-language (English here) because the
 * browser reads the manifest once at install time — there is no runtime i18n.
 *
 * @param options app-specific branding
 * @return a Next.js manifest object
 */
export function createManifest(options: CreateManifestOptions): MetadataRoute.Manifest {
  return {
    name: options.name,
    short_name: options.shortName,
    description: options.description,
    start_url: options.startUrl ?? "/",
    scope: options.scope ?? "/",
    display: "standalone",
    theme_color: options.themeColor,
    background_color: options.backgroundColor,
    icons: options.icons,
  };
}
