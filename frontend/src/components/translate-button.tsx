"use client";

import { useState } from "react";
import { Languages } from "lucide-react";
import { Tooltip, TooltipContent, TooltipTrigger } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { useTranslationConfig } from "@/lib/use-translation-config";
import { TranslateDialog } from "@/components/translate-dialog";

interface TranslateButtonProps {
  readonly text: string | null | undefined;
  /** Visual size of the icon — "sm" (h-3.5 w-3.5) for descriptions, "md" (h-4 w-4) for comments. */
  readonly size?: "sm" | "md";
}

/**
 * Renders a translate icon button next to a piece of text. Returns {@code null} when:
 * - the text is null, empty or whitespace-only,
 * - the translation feature is not configured on the backend, or
 * - the configuration probe is still in flight.
 *
 * Clicking the button opens a {@link TranslateDialog} that calls
 * {@code POST /api/translate} with the current UI language as target.
 */
export function TranslateButton({ text, size = "md" }: TranslateButtonProps) {
  const t = useTranslations();
  const { configured } = useTranslationConfig();
  const [open, setOpen] = useState(false);

  if (!text || text.trim().length === 0) {
    return null;
  }
  if (configured !== true) {
    return null;
  }

  const iconClass = size === "sm" ? "h-3.5 w-3.5" : "h-4 w-4";

  return (
    <>
      <Tooltip>
        <TooltipTrigger asChild>
          <button
            type="button"
            onClick={() => setOpen(true)}
            className="text-oe-gray-light hover:text-oe-dark shrink-0 ml-2"
            aria-label={t.translation.translate}
          >
            <Languages className={iconClass} />
          </button>
        </TooltipTrigger>
        <TooltipContent>{t.translation.translate}</TooltipContent>
      </Tooltip>
      <TranslateDialog
        open={open}
        onOpenChange={setOpen}
        sourceText={text}
      />
    </>
  );
}
