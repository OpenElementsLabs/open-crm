import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { act, renderHook } from "@testing-library/react";
import { useInstallPrompt } from "@/components/pwa/use-install-prompt";

function setUserAgent(ua: string) {
  Object.defineProperty(window.navigator, "userAgent", { value: ua, configurable: true });
}

function setStandalone(matches: boolean) {
  window.matchMedia = vi.fn().mockReturnValue({ matches } as MediaQueryList);
}

function fireBeforeInstallPrompt() {
  const event = new Event("beforeinstallprompt") as Event & {
    prompt: () => Promise<void>;
    userChoice: Promise<{ outcome: string }>;
  };
  event.prompt = vi.fn().mockResolvedValue(undefined);
  event.userChoice = Promise.resolve({ outcome: "accepted" });
  act(() => {
    window.dispatchEvent(event);
  });
  return event;
}

const DESKTOP_UA = "Mozilla/5.0 (Macintosh) Chrome/120";
const IOS_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Safari/604.1";

beforeEach(() => {
  setStandalone(false);
  setUserAgent(DESKTOP_UA);
});

afterEach(() => {
  vi.restoreAllMocks();
  setUserAgent(DESKTOP_UA);
});

describe("useInstallPrompt", () => {
  it("reports not installable before the browser offers a prompt", () => {
    const { result } = renderHook(() => useInstallPrompt());
    expect(result.current.canInstall).toBe(false);
    expect(result.current.isIos).toBe(false);
  });

  it("becomes installable after beforeinstallprompt fires", () => {
    const { result } = renderHook(() => useInstallPrompt());
    fireBeforeInstallPrompt();
    expect(result.current.canInstall).toBe(true);
  });

  it("triggers the saved prompt and then hides the affordance", async () => {
    const { result } = renderHook(() => useInstallPrompt());
    const event = fireBeforeInstallPrompt();

    await act(async () => {
      await result.current.promptInstall();
    });

    expect(event.prompt).toHaveBeenCalled();
    expect(result.current.canInstall).toBe(false);
  });

  it("hides the affordance once the app is installed", () => {
    const { result } = renderHook(() => useInstallPrompt());
    fireBeforeInstallPrompt();
    act(() => {
      window.dispatchEvent(new Event("appinstalled"));
    });
    expect(result.current.canInstall).toBe(false);
    expect(result.current.isStandalone).toBe(true);
  });

  it("returns the iOS hint state on iOS Safari when not installed", () => {
    setUserAgent(IOS_UA);
    const { result } = renderHook(() => useInstallPrompt());
    expect(result.current.isIos).toBe(true);
    expect(result.current.canInstall).toBe(false);
  });

  it("shows nothing when already running standalone", () => {
    setStandalone(true);
    setUserAgent(IOS_UA);
    const { result } = renderHook(() => useInstallPrompt());
    fireBeforeInstallPrompt();
    expect(result.current.isStandalone).toBe(true);
    expect(result.current.canInstall).toBe(false);
    expect(result.current.isIos).toBe(false);
  });
});
