import { describe, it, expect, afterEach, vi } from "vitest";
import { screen, cleanup, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "@/test/test-utils";
import { SidebarInstallAffordance } from "@/components/pwa/sidebar-install-affordance";
import { PwaInstallButton } from "@/components/pwa/pwa-install-button";
import { de as S } from "@/lib/i18n/de";

const mockPromptInstall = vi.fn();
const mockState = {
  canInstall: false,
  isIos: false,
  isStandalone: false,
  promptInstall: mockPromptInstall,
};

vi.mock("@/components/pwa/use-install-prompt", () => ({
  useInstallPrompt: () => mockState,
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
  Object.assign(mockState, { canInstall: false, isIos: false, isStandalone: false });
});

describe("SidebarInstallAffordance", () => {
  it("shows the install button when installable and triggers the prompt", () => {
    mockState.canInstall = true;
    renderWithProviders(<SidebarInstallAffordance />);

    const button = screen.getByText(S.pwa.install);
    expect(button).toBeInTheDocument();
    fireEvent.click(button);
    expect(mockPromptInstall).toHaveBeenCalled();
  });

  it("shows the iOS hint instead of a button on iOS", () => {
    mockState.isIos = true;
    renderWithProviders(<SidebarInstallAffordance />);

    expect(screen.getByText(S.pwa.iosHint)).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("renders nothing when not installable / already installed", () => {
    const { container } = renderWithProviders(<SidebarInstallAffordance />);
    expect(container).toBeEmptyDOMElement();
  });
});

describe("PwaInstallButton", () => {
  it("renders a clickable install button", () => {
    const onInstall = vi.fn();
    renderWithProviders(<PwaInstallButton variant="install" label="Install" onInstall={onInstall} />);
    fireEvent.click(screen.getByText("Install"));
    expect(onInstall).toHaveBeenCalled();
  });

  it("renders the iOS hint text without a button", () => {
    renderWithProviders(<PwaInstallButton variant="ios-hint" label="Install" hint="Share → Add" />);
    expect(screen.getByText("Share → Add")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
});
