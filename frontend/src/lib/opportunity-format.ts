/**
 * Formats an estimated value as EUR using the locale matching the UI language, or returns null
 * when there is no value (callers render a dash). Spec 114: "25.000,00 €" in DE, "€25,000.00" in EN.
 */
export function formatEur(value: number | null | undefined, language: string): string | null {
  if (value === null || value === undefined) {
    return null;
  }
  return new Intl.NumberFormat(language === "de" ? "de-DE" : "en-US", {
    style: "currency",
    currency: "EUR",
  }).format(value);
}
