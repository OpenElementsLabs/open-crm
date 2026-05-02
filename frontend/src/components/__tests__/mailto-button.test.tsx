import { describe, it, expect, afterEach, beforeEach, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MailtoButton } from "@/components/mailto-button";

const originalLocation = window.location;

beforeEach(() => {
  Object.defineProperty(window, "location", {
    writable: true,
    value: { ...originalLocation, href: "" },
  });
});

afterEach(() => {
  Object.defineProperty(window, "location", { writable: true, value: originalLocation });
  cleanup();
});

describe("MailtoButton", () => {
  it("renders the mail icon", () => {
    const { container } = render(<MailtoButton email="x@y.z" />);
    expect(container.querySelector("svg.lucide-mail")).toBeInTheDocument();
  });

  it("navigates to the mailto: URL on click", () => {
    render(<MailtoButton email="user@example.com" />);
    fireEvent.click(screen.getByRole("button"));
    expect(window.location.href).toBe("mailto:user@example.com");
  });
});
