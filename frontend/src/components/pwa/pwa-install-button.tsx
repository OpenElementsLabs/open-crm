"use client";

import { Download, Share } from "lucide-react";
import { Button } from "@open-elements/ui";

/**
 * GENERIC — extraction target for `@open-elements/ui`.
 *
 * Presentational install affordance. Renders either a native-install button or an iOS instruction
 * hint. All copy is passed in; it holds no install logic (that lives in `useInstallPrompt`).
 */
export interface PwaInstallButtonProps {
  /** `install` shows a clickable button; `ios-hint` shows the "Add to Home Screen" hint. */
  readonly variant: "install" | "ios-hint";
  /** Label for the install button. */
  readonly label: string;
  /** Hint text for the iOS variant. */
  readonly hint?: string;
  /** Invoked when the install button is clicked. */
  readonly onInstall?: () => void;
}

export function PwaInstallButton({ variant, label, hint, onInstall }: PwaInstallButtonProps) {
  if (variant === "ios-hint") {
    return (
      <div className="flex items-start gap-2 rounded-md border border-oe-gray-light bg-oe-gray-light/20 px-3 py-2 text-xs text-oe-gray-mid">
        <Share className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
        <span>{hint}</span>
      </div>
    );
  }
  return (
    <Button variant="outline" size="sm" className="w-full justify-start" onClick={onInstall}>
      <Download className="mr-2 h-4 w-4" />
      {label}
    </Button>
  );
}
