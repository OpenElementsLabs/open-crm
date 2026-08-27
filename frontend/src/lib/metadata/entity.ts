import type { Metadata } from "next";
import type { CompanyDto, ContactDto } from "@/lib/types";
import { SITE_DESCRIPTION, titleFor } from "./site";
import { normalizeDescription } from "./text";

/**
 * Entity-specific metadata helpers: display names, initials, secondary lines, the `Metadata`
 * objects returned by `generateMetadata`, and the view-models consumed by the Open Graph image
 * routes. All are pure functions of their inputs so they can be unit-tested without rendering.
 */

/** The portrait/name/secondary-line data an Open Graph image route needs to render one variant. */
export interface OgImageModel {
  /** The large display name. */
  readonly name: string;
  /** The 1–2 letter initials shown when no image is available. */
  readonly initials: string;
  /** The secondary line under the name, or `null` when there is nothing to show. */
  readonly secondaryLine: string | null;
  /** A `data:` URI for the photo/logo, or `null` to render the initials variant. */
  readonly imageSrc: string | null;
}

function collapseSpaces(value: string): string {
  return value.replace(/\s+/g, " ").trim();
}

// --- Contact -----------------------------------------------------------------

/**
 * The contact's display name: academic title, first and last name joined and space-collapsed, so a
 * missing title never leaves a double space — e.g. `Dr. Max Mustermann` or `Max Mustermann`.
 */
export function contactDisplayName(contact: Pick<ContactDto, "title" | "firstName" | "lastName">): string {
  return collapseSpaces([contact.title, contact.firstName, contact.lastName].filter(Boolean).join(" "));
}

/** First letters of the first and last name, upper-cased — e.g. `MM`. */
export function contactInitials(contact: Pick<ContactDto, "firstName" | "lastName">): string {
  return [contact.firstName, contact.lastName]
    .map((part) => part?.trim()?.[0] ?? "")
    .join("")
    .toUpperCase();
}

/**
 * The secondary line for a contact: position and company, whichever are present, joined with an
 * en dash. Returns `null` when neither exists, so no empty line is drawn.
 */
export function contactSecondaryLine(
  contact: Pick<ContactDto, "position" | "companyName">,
): string | null {
  const parts = [contact.position, contact.companyName].map((p) => p?.trim()).filter(Boolean);
  return parts.length > 0 ? parts.join(" · ") : null;
}

/**
 * The `Metadata` for a contact detail page. When `contact` is `null` (not found, forbidden, or a
 * failed fetch) an empty object is returned so the root title and description apply and nothing
 * about the entity leaks into the HTML.
 */
export function buildContactMetadata(contact: ContactDto | null): Metadata {
  if (!contact) {
    return {};
  }
  const name = contactDisplayName(contact);
  const description = normalizeDescription(contact.description);
  return buildDetailMetadata(name, description);
}

/** The Open Graph image view-model for a contact. */
export function contactImageModel(contact: ContactDto | null, imageSrc: string | null): OgImageModel {
  if (!contact) {
    return { name: "Open CRM", initials: "", secondaryLine: null, imageSrc: null };
  }
  return {
    name: contactDisplayName(contact),
    initials: contactInitials(contact),
    secondaryLine: contactSecondaryLine(contact),
    imageSrc: contact.hasPhoto ? imageSrc : null,
  };
}

// --- Company -----------------------------------------------------------------

/** The company's display name. */
export function companyDisplayName(company: Pick<CompanyDto, "name">): string {
  return collapseSpaces(company.name);
}

/**
 * The first letters of the first two words of the company name, upper-cased — e.g. `Acme GmbH` →
 * `AG`, `Acme` → `A`.
 */
export function companyInitials(company: Pick<CompanyDto, "name">): string {
  return collapseSpaces(company.name)
    .split(" ")
    .slice(0, 2)
    .map((word) => word[0] ?? "")
    .join("")
    .toUpperCase();
}

/**
 * The secondary line for a company: city and country, whichever are present, joined with a comma.
 * Returns `null` when neither exists.
 */
export function companySecondaryLine(
  company: Pick<CompanyDto, "city" | "country">,
): string | null {
  const parts = [company.city, company.country].map((p) => p?.trim()).filter(Boolean);
  return parts.length > 0 ? parts.join(", ") : null;
}

/** The `Metadata` for a company detail page; empty object when `company` is `null` (see above). */
export function buildCompanyMetadata(company: CompanyDto | null): Metadata {
  if (!company) {
    return {};
  }
  const name = companyDisplayName(company);
  const description = normalizeDescription(company.description);
  return buildDetailMetadata(name, description);
}

/** The Open Graph image view-model for a company. */
export function companyImageModel(company: CompanyDto | null, imageSrc: string | null): OgImageModel {
  if (!company) {
    return { name: "Open CRM", initials: "", secondaryLine: null, imageSrc: null };
  }
  return {
    name: companyDisplayName(company),
    initials: companyInitials(company),
    secondaryLine: companySecondaryLine(company),
    imageSrc: company.hasLogo ? imageSrc : null,
  };
}

// --- Shared ------------------------------------------------------------------

/**
 * Assembles the shared `Metadata` shape for a detail page: the bare title (the root template adds
 * the `Open CRM: ` prefix), the optional page description, and Open Graph / Twitter tags whose
 * titles match the full document title. `og:image` / `twitter:image` are contributed automatically
 * by each route's `opengraph-image` file convention, so they are not set here.
 */
function buildDetailMetadata(name: string, description: string | undefined): Metadata {
  const fullTitle = titleFor(name);
  return {
    title: name,
    description,
    openGraph: {
      title: fullTitle,
      description: description ?? SITE_DESCRIPTION,
      type: "website",
    },
    twitter: {
      card: "summary_large_image",
      title: fullTitle,
      description: description ?? SITE_DESCRIPTION,
    },
  };
}
