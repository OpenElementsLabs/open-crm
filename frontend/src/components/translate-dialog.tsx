"use client";

import { useEffect, useState } from "react";
import { Check, Copy, Loader2 } from "lucide-react";
import { Button, Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@open-elements/ui";
import { useLanguage, useTranslations } from "@/lib/i18n";
import { translateText } from "@/lib/api";

interface TranslateDialogProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly sourceText: string;
}

export function TranslateDialog({ open, onOpenChange, sourceText }: TranslateDialogProps) {
  const t = useTranslations();
  const T = t.translation;
  const { language } = useLanguage();
  const targetLanguage = language === "de" ? "de" : "en";

  const [loading, setLoading] = useState(false);
  const [translated, setTranslated] = useState<string | null>(null);
  const [error, setError] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    setLoading(true);
    setTranslated(null);
    setError(false);
    setCopied(false);
    let cancelled = false;
    translateText(sourceText, targetLanguage)
      .then((result) => {
        if (!cancelled) {
          setTranslated(result.translatedText);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError(true);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [open, sourceText, targetLanguage]);

  const handleCopy = async () => {
    if (!translated) return;
    try {
      await navigator.clipboard.writeText(translated);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard not available; ignore.
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="font-heading">{T.title}</DialogTitle>
        </DialogHeader>
        <div className="min-h-[80px] py-2">
          {loading && (
            <div className="flex items-center gap-2 text-sm text-oe-gray-mid">
              <Loader2 className="h-4 w-4 animate-spin" />
              {T.loading}
            </div>
          )}
          {!loading && error && (
            <p className="text-sm text-oe-red">{T.error}</p>
          )}
          {!loading && !error && translated && (
            <p className="text-sm text-oe-black whitespace-pre-line break-words">{translated}</p>
          )}
        </div>
        <DialogFooter>
          <Button
            variant="outline"
            onClick={handleCopy}
            disabled={loading || error || !translated}
          >
            {copied ? (
              <>
                <Check className="mr-2 h-4 w-4" />
                {T.copied}
              </>
            ) : (
              <>
                <Copy className="mr-2 h-4 w-4" />
                {T.copy}
              </>
            )}
          </Button>
          <Button onClick={() => onOpenChange(false)}>{T.close}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
