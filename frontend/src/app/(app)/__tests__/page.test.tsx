import { describe, it, expect, vi, afterEach } from "vitest";

const redirectMock = vi.fn();

vi.mock("next/navigation", () => ({
  redirect: (target: string) => redirectMock(target),
}));

afterEach(() => {
  redirectMock.mockClear();
});

describe("(app)/page", () => {
  it("redirects to /updates as the default landing route", async () => {
    const { default: Home } = await import("../page");
    Home();
    expect(redirectMock).toHaveBeenCalledTimes(1);
    expect(redirectMock).toHaveBeenCalledWith("/updates");
  });
});
