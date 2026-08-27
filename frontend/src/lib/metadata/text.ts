/**
 * Pure text helpers for building HTML metadata. Kept free of any React or Next.js dependency so
 * they can be unit-tested directly.
 */

/** Maximum length of a `<meta name="description">`, including a trailing ellipsis. */
export const MAX_DESCRIPTION_LENGTH = 160;

const ELLIPSIS = "…";

/**
 * Deletes non-whitespace C0/C1 control characters. Whitespace controls (tab U+0009, newline
 * U+000A, carriage return U+000D) are intentionally kept here — they are collapsed to a single
 * space by the later `\s+` pass, so "line one\nline two" becomes "line one line two", not
 * "line oneline two". Implemented by code point (rather than a control-character regex) so the
 * source file contains no literal control bytes.
 */
function stripNonWhitespaceControls(text: string): string {
  let result = "";
  for (const char of text) {
    const code = char.codePointAt(0) ?? 0;
    const isWhitespaceControl = code === 0x09 || code === 0x0a || code === 0x0d;
    const isControl = code <= 0x1f || (code >= 0x7f && code <= 0x9f);
    if (isControl && !isWhitespaceControl) {
      continue;
    }
    result += char;
  }
  return result;
}

/**
 * Truncates `text` to at most `max` characters at a word boundary, appending an ellipsis when the
 * text is cut. If a single word already exceeds the limit, it is hard-cut so the result never
 * exceeds `max`. Assumes `text` is already whitespace-collapsed and trimmed.
 */
export function truncateAtWordBoundary(text: string, max: number = MAX_DESCRIPTION_LENGTH): string {
  if (text.length <= max) {
    return text;
  }
  // Reserve room for the ellipsis, then cut back to the last space so we never end mid-word.
  const hardLimit = max - ELLIPSIS.length;
  const slice = text.slice(0, hardLimit);
  const lastSpace = slice.lastIndexOf(" ");
  const body = lastSpace > 0 ? slice.slice(0, lastSpace) : slice;
  return `${body.trimEnd()}${ELLIPSIS}`;
}

/**
 * Normalises a free-text entity description into a value safe and sensible for a `<meta>` tag:
 * deletes non-whitespace control characters, collapses every run of whitespace (including newlines
 * and tabs) to a single space, trims, and truncates to {@link MAX_DESCRIPTION_LENGTH} at a word
 * boundary.
 *
 * Returns `undefined` when the input is null, empty, or whitespace-only — signalling the caller to
 * inherit the application-level description rather than emit an empty one.
 */
export function normalizeDescription(raw: string | null | undefined): string | undefined {
  if (!raw) {
    return undefined;
  }
  const collapsed = stripNonWhitespaceControls(raw)
    .replace(/\s+/g, " ")
    .trim();
  if (collapsed.length === 0) {
    return undefined;
  }
  return truncateAtWordBoundary(collapsed);
}
