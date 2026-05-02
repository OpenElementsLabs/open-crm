import { describe, it, expect, afterEach, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { PrimaryButton } from "@/components/primary-button";

afterEach(() => {
  cleanup();
});

describe("PrimaryButton", () => {
  it("renders children", () => {
    render(<PrimaryButton>Save</PrimaryButton>);
    expect(screen.getByRole("button", { name: "Save" })).toBeInTheDocument();
  });

  it("applies the brand-green primary class", () => {
    render(<PrimaryButton>Go</PrimaryButton>);
    const button = screen.getByRole("button");
    expect(button.className).toContain("bg-oe-green");
    expect(button.className).toContain("hover:bg-oe-green-dark");
    expect(button.className).toContain("text-white");
  });

  it("merges caller-supplied className alongside the primary class", () => {
    render(<PrimaryButton className="mt-2 w-full">Go</PrimaryButton>);
    const button = screen.getByRole("button");
    expect(button.className).toContain("bg-oe-green");
    expect(button.className).toContain("mt-2");
    expect(button.className).toContain("w-full");
  });

  it("forwards onClick", () => {
    const handleClick = vi.fn();
    render(<PrimaryButton onClick={handleClick}>Go</PrimaryButton>);
    fireEvent.click(screen.getByRole("button"));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it("renders as the child element when asChild is set", () => {
    render(
      <PrimaryButton asChild>
        <a href="/somewhere">Link</a>
      </PrimaryButton>,
    );
    const link = screen.getByRole("link", { name: "Link" });
    expect(link).toHaveAttribute("href", "/somewhere");
    expect(link.className).toContain("bg-oe-green");
  });
});
