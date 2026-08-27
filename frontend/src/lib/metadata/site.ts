/**
 * Branding constants for document and social metadata. The `Open CRM: ` prefix is a branding
 * decision that must live in exactly one place — the root layout's title template and the
 * per-page Open Graph / Twitter titles both derive it from here so they can never drift apart.
 */

/** The application name, used as the bare title and as the title-template prefix. */
export const SITE_NAME = "Open CRM";

/** The Next.js title template applied to every page that sets its own title. */
export const TITLE_TEMPLATE = `${SITE_NAME}: %s`;

/** The root/application description inherited by pages that set none of their own. */
export const SITE_DESCRIPTION = "CRM system by Open Elements";

/**
 * The full document title for a named entity, e.g. `Open CRM: Max Mustermann`. This is what the
 * browser tab shows (via {@link TITLE_TEMPLATE}) and therefore what `og:title` / `twitter:title`
 * must equal.
 */
export function titleFor(name: string): string {
  return `${SITE_NAME}: ${name}`;
}
