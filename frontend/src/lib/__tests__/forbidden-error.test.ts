import { describe, it, expect } from "vitest";
import { ForbiddenError } from "@/lib/forbidden-error";

describe("ForbiddenError", () => {
  it("is an Error subclass", () => {
    expect(new ForbiddenError()).toBeInstanceOf(Error);
  });

  it("carries the expected name", () => {
    expect(new ForbiddenError().name).toBe("ForbiddenError");
  });

  it("uses 'Forbidden' as the default message", () => {
    expect(new ForbiddenError().message).toBe("Forbidden");
  });

  it("accepts a custom message", () => {
    expect(new ForbiddenError("custom").message).toBe("custom");
  });

  it("can be caught as a ForbiddenError via instanceof", () => {
    try {
      throw new ForbiddenError();
    } catch (e) {
      expect(e instanceof ForbiddenError).toBe(true);
    }
  });
});
