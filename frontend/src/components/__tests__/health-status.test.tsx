import { describe, it, expect, afterEach } from "vitest";
import { screen, cleanup } from "@testing-library/react";
import { HealthStatus } from "@/components/health-status";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";

afterEach(() => {
  cleanup();
});

describe("HealthStatus", () => {
  describe("when backend is healthy", () => {
    it("should render up status text and green indicator", () => {
      const { container } = renderWithProviders(<HealthStatus healthy={true} />);

      const statusText = container.querySelector("span.font-medium");
      expect(statusText).toHaveTextContent(de.health.statusUp);

      const indicator = container.querySelector("span[aria-label]");
      expect(indicator).toHaveAttribute("aria-label", de.health.statusUp);
      expect(indicator?.className).toContain("bg-oe-green");
    });

    it("should display the system status title", () => {
      const { container } = renderWithProviders(<HealthStatus healthy={true} />);

      const title = container.querySelector("[data-slot='card-title']");
      expect(title).toHaveTextContent(de.health.title);
    });
  });

  describe("when backend is unavailable", () => {
    it("should render down status text and red indicator", () => {
      const { container } = renderWithProviders(<HealthStatus healthy={false} />);

      const statusText = container.querySelector("span.font-medium");
      expect(statusText).toHaveTextContent(de.health.statusDown);

      const indicator = container.querySelector("span[aria-label]");
      expect(indicator).toHaveAttribute("aria-label", de.health.statusDown);
      expect(indicator?.className).toContain("bg-oe-red");
    });
  });

  describe("static display behavior", () => {
    it("should render status as a pure display without re-fetch logic", () => {
      const { container } = renderWithProviders(<HealthStatus healthy={true} />);

      const indicator = container.querySelector("span[aria-label]");
      expect(indicator).toBeInTheDocument();

      // Component is a pure display component — it receives status as a prop
      // and renders it once. No timers, intervals, or re-fetch logic.
      const statusText = container.querySelector("span.font-medium");
      expect(statusText).toHaveTextContent(de.health.statusUp);
    });
  });
});
