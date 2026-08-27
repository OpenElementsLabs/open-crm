import { describe, expect, it } from "vitest";
import type { CompanyDto, ContactDto } from "@/lib/types";
import {
  buildCompanyMetadata,
  buildContactMetadata,
  companyImageModel,
  companyInitials,
  companySecondaryLine,
  contactDisplayName,
  contactImageModel,
  contactInitials,
  contactSecondaryLine,
} from "./entity";
import { SITE_DESCRIPTION } from "./site";

function contact(overrides: Partial<ContactDto> = {}): ContactDto {
  return {
    id: "c1",
    title: null,
    firstName: "Max",
    lastName: "Mustermann",
    email: null,
    position: null,
    gender: null,
    socialLinks: [],
    phoneNumber: null,
    description: null,
    companyId: null,
    companyName: null,
    commentCount: 0,
    hasPhoto: false,
    birthday: null,
    brevo: false,
    receivesNewsletter: false,
    language: null,
    tagIds: [],
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

function company(overrides: Partial<CompanyDto> = {}): CompanyDto {
  return {
    id: "k1",
    name: "Acme GmbH",
    email: null,
    website: null,
    street: null,
    houseNumber: null,
    zipCode: null,
    city: null,
    country: null,
    phoneNumber: null,
    description: null,
    bankName: null,
    bic: null,
    iban: null,
    vatId: null,
    brevo: false,
    hasLogo: false,
    contactCount: 0,
    commentCount: 0,
    tagIds: [],
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

describe("contact helpers", () => {
  it("composes the display name with an academic title", () => {
    expect(contactDisplayName(contact({ title: "Dr." }))).toBe("Dr. Max Mustermann");
  });

  it("leaves no double space when the title is absent", () => {
    expect(contactDisplayName(contact({ title: null }))).toBe("Max Mustermann");
    expect(contactDisplayName(contact({ title: "" }))).toBe("Max Mustermann");
  });

  it("derives two-letter initials", () => {
    expect(contactInitials(contact())).toBe("MM");
  });

  it("builds the secondary line from position and company, or null when absent", () => {
    expect(contactSecondaryLine(contact({ position: "Head of Sales", companyName: "Acme GmbH" }))).toBe(
      "Head of Sales · Acme GmbH",
    );
    expect(contactSecondaryLine(contact({ position: null, companyName: null }))).toBeNull();
  });
});

describe("company helpers", () => {
  it("uses the company name as the display name", () => {
    expect(companyInitials(company({ name: "Acme GmbH" }))).toBe("AG");
  });

  it("yields a single initial for a one-word name", () => {
    expect(companyInitials(company({ name: "Acme" }))).toBe("A");
  });

  it("builds the secondary line from city and country, or null when absent", () => {
    expect(companySecondaryLine(company({ city: "Berlin", country: "Germany" }))).toBe("Berlin, Germany");
    expect(companySecondaryLine(company({ city: null, country: null }))).toBeNull();
  });
});

describe("buildContactMetadata", () => {
  it("returns an empty object for a null contact so the root metadata applies", () => {
    expect(buildContactMetadata(null)).toEqual({});
  });

  it("sets the bare title, meta description and matching OG/Twitter titles", () => {
    const meta = buildContactMetadata(
      contact({ title: "Dr.", description: "Ansprechpartner Einkauf." }),
    );
    expect(meta.title).toBe("Dr. Max Mustermann");
    expect(meta.description).toBe("Ansprechpartner Einkauf.");
    expect(meta.openGraph?.title).toBe("Open CRM: Dr. Max Mustermann");
    expect(meta.twitter?.title).toBe("Open CRM: Dr. Max Mustermann");
    expect(meta.openGraph?.description).toBe("Ansprechpartner Einkauf.");
  });

  it("omits the page description and falls back to the site description in OG when none exists", () => {
    const meta = buildContactMetadata(contact({ description: "   " }));
    expect(meta.description).toBeUndefined();
    expect(meta.openGraph?.description).toBe(SITE_DESCRIPTION);
  });
});

describe("buildCompanyMetadata", () => {
  it("returns an empty object for a null company", () => {
    expect(buildCompanyMetadata(null)).toEqual({});
  });

  it("sets the company title and description", () => {
    const meta = buildCompanyMetadata(company({ description: "Maschinenbau." }));
    expect(meta.title).toBe("Acme GmbH");
    expect(meta.openGraph?.title).toBe("Open CRM: Acme GmbH");
    expect(meta.description).toBe("Maschinenbau.");
  });
});

describe("image models", () => {
  it("uses the photo when the contact has one", () => {
    const model = contactImageModel(contact({ hasPhoto: true }), "data:image/png;base64,AAA");
    expect(model.imageSrc).toBe("data:image/png;base64,AAA");
    expect(model.initials).toBe("MM");
  });

  it("ignores a provided image when the contact has no photo", () => {
    const model = contactImageModel(contact({ hasPhoto: false }), "data:image/png;base64,AAA");
    expect(model.imageSrc).toBeNull();
  });

  it("renders a neutral model for a missing entity", () => {
    expect(contactImageModel(null, "data:image/png;base64,AAA")).toEqual({
      name: "Open CRM",
      initials: "",
      secondaryLine: null,
      imageSrc: null,
    });
  });

  it("uses the logo when the company has one, else falls back to initials", () => {
    expect(companyImageModel(company({ hasLogo: true }), "data:image/png;base64,BBB").imageSrc).toBe(
      "data:image/png;base64,BBB",
    );
    expect(companyImageModel(company({ hasLogo: false }), "data:image/png;base64,BBB").imageSrc).toBeNull();
  });
});
