import { beforeEach, describe, expect, it, vi } from "vitest";
import type { Mock } from "vitest";

vi.mock("@/lib/cached-entities", () => ({
  getCachedContact: vi.fn(),
  getCachedCompany: vi.fn(),
}));
vi.mock("@/components/company-detail", () => ({ CompanyDetail: () => null }));

import { getCachedCompany } from "@/lib/cached-entities";
import { generateMetadata } from "./page";

const mockGetCompany = getCachedCompany as unknown as Mock;

function params(id: string) {
  return { params: Promise.resolve({ id }) };
}

describe("company page generateMetadata", () => {
  beforeEach(() => {
    mockGetCompany.mockReset();
  });

  it("builds title and description from the company", async () => {
    mockGetCompany.mockResolvedValue({
      name: "Acme GmbH",
      description: "Maschinenbau.",
      city: "Berlin",
      country: "Germany",
      hasLogo: false,
    });

    const meta = await generateMetadata(params("k1"));

    expect(meta.title).toBe("Acme GmbH");
    expect(meta.description).toBe("Maschinenbau.");
    expect(meta.openGraph?.title).toBe("Open CRM: Acme GmbH");
  });

  it("returns empty metadata and never throws when the fetch fails", async () => {
    mockGetCompany.mockRejectedValue(new Error("500"));

    const meta = await generateMetadata(params("missing"));

    expect(meta).toEqual({});
  });
});
