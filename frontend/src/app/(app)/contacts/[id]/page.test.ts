import { beforeEach, describe, expect, it, vi } from "vitest";
import type { Mock } from "vitest";

vi.mock("@/lib/cached-entities", () => ({
  getCachedContact: vi.fn(),
  getCachedCompany: vi.fn(),
}));
vi.mock("@/components/contact-detail", () => ({ ContactDetail: () => null }));

import { getCachedContact } from "@/lib/cached-entities";
import { generateMetadata } from "./page";

const mockGetContact = getCachedContact as unknown as Mock;

function params(id: string) {
  return { params: Promise.resolve({ id }) };
}

describe("contact page generateMetadata", () => {
  beforeEach(() => {
    mockGetContact.mockReset();
  });

  it("builds title and description from the contact", async () => {
    mockGetContact.mockResolvedValue({
      title: "Dr.",
      firstName: "Max",
      lastName: "Mustermann",
      description: "Einkauf.",
      position: null,
      companyName: null,
      hasPhoto: false,
    });

    const meta = await generateMetadata(params("c1"));

    expect(meta.title).toBe("Dr. Max Mustermann");
    expect(meta.description).toBe("Einkauf.");
    expect(meta.openGraph?.title).toBe("Open CRM: Dr. Max Mustermann");
  });

  it("returns empty metadata and never throws when the fetch fails", async () => {
    mockGetContact.mockRejectedValue(new Error("500"));

    const meta = await generateMetadata(params("missing"));

    expect(meta).toEqual({});
  });
});
