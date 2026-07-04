"use client";

import { useTranslations } from "@/lib/i18n";
import { useInstallPrompt } from "./use-install-prompt";
import { PwaInstallButton } from "./pwa-install-button";

/**
 * APP-SPECIFIC wiring: places the install affordance in the sidebar footer and supplies i18n copy.
 * Shows the native-install button when the browser offered one, an iOS "Add to Home Screen" hint on
 * iOS Safari, and nothing once installed or when installation is unavailable.
 */
export function SidebarInstallAffordance() {
  const t = useTranslations();
  const { canInstall, isIos, promptInstall } = useInstallPrompt();

  if (canInstall) {
    return (
      <div className="px-3 py-2">
        <PwaInstallButton variant="install" label={t.pwa.install} onInstall={() => void promptInstall()} />
      </div>
    );
  }
  if (isIos) {
    return (
      <div className="px-3 py-2">
        <PwaInstallButton variant="ios-hint" label={t.pwa.install} hint={t.pwa.iosHint} />
      </div>
    );
  }
  return null;
}
