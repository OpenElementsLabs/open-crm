"use client";

import { useEffect, useState } from "react";

/**
 * GENERIC — extraction target for `@open-elements/ui`.
 *
 * Captures the `beforeinstallprompt` event, tracks install/standalone state, detects iOS Safari (which
 * never fires the event), and exposes a `promptInstall()` that triggers the saved native prompt. No
 * persistence — the state resets each page load, so the affordance reappears every session.
 */

/** The non-standard `beforeinstallprompt` event. */
interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  readonly userChoice: Promise<{ outcome: "accepted" | "dismissed" }>;
}

export interface InstallPromptState {
  /** True when the browser offered a native install prompt and the app is not installed. */
  readonly canInstall: boolean;
  /** True on iOS Safari when not already installed (no native prompt available). */
  readonly isIos: boolean;
  /** True when the app is already running as an installed PWA. */
  readonly isStandalone: boolean;
  /** Triggers the saved native prompt; no-op if none is available. */
  readonly promptInstall: () => Promise<void>;
}

function detectStandalone(): boolean {
  if (typeof window === "undefined") {
    return false;
  }
  const standaloneDisplay =
    typeof window.matchMedia === "function" && window.matchMedia("(display-mode: standalone)").matches;
  const iosStandalone = (window.navigator as Navigator & { standalone?: boolean }).standalone === true;
  return standaloneDisplay || iosStandalone;
}

function detectIos(): boolean {
  if (typeof window === "undefined") {
    return false;
  }
  const ua = window.navigator.userAgent;
  const isIosDevice = /iphone|ipad|ipod/i.test(ua);
  // Exclude in-app browsers / Chrome-on-iOS is still WebKit, but only Safari supports "Add to Home
  // Screen"; treat any iOS WebKit as the hint case, which is the safe default.
  return isIosDevice;
}

export function useInstallPrompt(): InstallPromptState {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [isStandalone, setIsStandalone] = useState(false);
  const [isIos, setIsIos] = useState(false);

  useEffect(() => {
    setIsStandalone(detectStandalone());
    setIsIos(detectIos());

    const onBeforeInstallPrompt = (event: Event) => {
      event.preventDefault();
      setDeferredPrompt(event as BeforeInstallPromptEvent);
    };
    const onAppInstalled = () => {
      setDeferredPrompt(null);
      setIsStandalone(true);
    };

    window.addEventListener("beforeinstallprompt", onBeforeInstallPrompt);
    window.addEventListener("appinstalled", onAppInstalled);
    return () => {
      window.removeEventListener("beforeinstallprompt", onBeforeInstallPrompt);
      window.removeEventListener("appinstalled", onAppInstalled);
    };
  }, []);

  const promptInstall = async (): Promise<void> => {
    if (!deferredPrompt) {
      return;
    }
    await deferredPrompt.prompt();
    await deferredPrompt.userChoice;
    // A saved prompt can only be used once.
    setDeferredPrompt(null);
  };

  return {
    canInstall: deferredPrompt !== null && !isStandalone,
    isIos: isIos && !isStandalone,
    isStandalone,
    promptInstall,
  };
}
