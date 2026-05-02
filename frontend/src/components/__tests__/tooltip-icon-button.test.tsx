import { describe, it, expect, afterEach, vi } from "vitest";
import { Pencil, Trash2 } from "lucide-react";
import { cleanup, fireEvent, screen } from "@testing-library/react";
import { TooltipIconButton } from "@/components/tooltip-icon-button";
import { renderWithProviders } from "@/test/test-utils";

afterEach(() => {
  cleanup();
});

describe("TooltipIconButton", () => {
  it("renders the icon", () => {
    const { container } = renderWithProviders(
      <TooltipIconButton icon={<Pencil />} tooltip="Edit" onClick={() => {}} />,
    );
    expect(container.querySelector("svg.lucide-pencil")).toBeInTheDocument();
  });

  it("uses the tooltip string as the button's accessible name", () => {
    renderWithProviders(
      <TooltipIconButton icon={<Pencil />} tooltip="Edit user" onClick={() => {}} />,
    );
    expect(screen.getByRole("button", { name: "Edit user" })).toBeInTheDocument();
  });

  it("calls onClick when clicked", () => {
    const handleClick = vi.fn();
    renderWithProviders(
      <TooltipIconButton icon={<Pencil />} tooltip="Edit" onClick={handleClick} />,
    );
    fireEvent.click(screen.getByRole("button"));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it("stops click propagation so parent handlers do not fire", () => {
    const parentHandler = vi.fn();
    renderWithProviders(
      <div onClick={parentHandler}>
        <TooltipIconButton icon={<Pencil />} tooltip="Edit" onClick={() => {}} />
      </div>,
    );
    fireEvent.click(screen.getByRole("button"));
    expect(parentHandler).not.toHaveBeenCalled();
  });

  it("applies the destructive tone class", () => {
    renderWithProviders(
      <TooltipIconButton
        icon={<Trash2 />}
        tooltip="Delete"
        tone="destructive"
        onClick={() => {}}
      />,
    );
    expect(screen.getByRole("button").className).toContain("text-oe-red");
  });

  it("applies the default tone class when tone is omitted", () => {
    renderWithProviders(
      <TooltipIconButton icon={<Pencil />} tooltip="Edit" onClick={() => {}} />,
    );
    expect(screen.getByRole("button").className).toContain("text-oe-green");
  });

  it("disables the button when disabled is true and does not fire onClick", () => {
    const handleClick = vi.fn();
    renderWithProviders(
      <TooltipIconButton
        icon={<Trash2 />}
        tooltip="Cannot delete"
        disabled
        onClick={handleClick}
      />,
    );
    const button = screen.getByRole("button");
    expect(button).toBeDisabled();
    fireEvent.click(button);
    expect(handleClick).not.toHaveBeenCalled();
  });

  it("renders a Link when href is provided", () => {
    const { container } = renderWithProviders(
      <TooltipIconButton icon={<Pencil />} tooltip="Edit tag" href="/tags/123/edit" />,
    );
    const anchor = container.querySelector("a");
    expect(anchor).toHaveAttribute("href", "/tags/123/edit");
    expect(anchor?.querySelector("svg.lucide-pencil")).toBeInTheDocument();
  });
});
