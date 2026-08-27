import { describe, expect, it } from "vitest";
import { MAX_DESCRIPTION_LENGTH, normalizeDescription, truncateAtWordBoundary } from "./text";

describe("truncateAtWordBoundary", () => {
  it("returns text unchanged when within the limit", () => {
    expect(truncateAtWordBoundary("short text", 160)).toBe("short text");
  });

  it("truncates at a word boundary and appends an ellipsis", () => {
    const text = "the quick brown fox jumps over the lazy dog";
    const result = truncateAtWordBoundary(text, 20);
    expect(result.length).toBeLessThanOrEqual(20);
    expect(result.endsWith("…")).toBe(true);
    // No partial word before the ellipsis.
    expect(text.startsWith(result.slice(0, -1).trimEnd())).toBe(true);
  });

  it("hard-cuts a single word longer than the limit", () => {
    const result = truncateAtWordBoundary("supercalifragilisticexpialidocious", 10);
    expect(result.length).toBeLessThanOrEqual(10);
    expect(result.endsWith("…")).toBe(true);
  });
});

describe("normalizeDescription", () => {
  it("returns undefined for null, empty, or whitespace-only input", () => {
    expect(normalizeDescription(null)).toBeUndefined();
    expect(normalizeDescription(undefined)).toBeUndefined();
    expect(normalizeDescription("")).toBeUndefined();
    expect(normalizeDescription("   \n\t  ")).toBeUndefined();
  });

  it("passes a normal description through unchanged", () => {
    const text = "Langjähriger Ansprechpartner für den Bereich Einkauf.";
    expect(normalizeDescription(text)).toBe(text);
  });

  it("collapses newlines, tabs and runs of spaces to single spaces and trims", () => {
    expect(normalizeDescription("  line one\n\tline   two  \r\n line three ")).toBe(
      "line one line two line three",
    );
  });

  it("strips non-whitespace control characters but keeps whitespace controls as spaces", () => {
    const nul = String.fromCharCode(0);
    const bell = String.fromCharCode(7);
    const del = String.fromCharCode(127);
    const tab = String.fromCharCode(9);
    // "a<NUL><BEL>b<DEL>c<TAB>d" -> controls deleted, tab becomes a space.
    const input = `a${nul}${bell}b${del}c${tab}d`;
    expect(normalizeDescription(input)).toBe("abc d");
  });

  it("truncates a description longer than the maximum at a word boundary", () => {
    const long = Array.from({ length: 60 }, (_, i) => `word${i}`).join(" ");
    const result = normalizeDescription(long)!;
    expect(result.length).toBeLessThanOrEqual(MAX_DESCRIPTION_LENGTH);
    expect(result.endsWith("…")).toBe(true);
    // The body (without the ellipsis) is an exact prefix of the original, cut at a word boundary:
    // the next character in the original is a space, i.e. no word was split.
    const body = result.slice(0, -1);
    expect(long.startsWith(body)).toBe(true);
    expect(long[body.length]).toBe(" ");
  });
});
