import type { OpportunityStatus } from "./types";

/**
 * Frontend-only value lists for opportunity `stage` and `product`. Spec 113 stores both as free
 * strings; these suggestion lists live only here so the later switch to Kanban-provided stage
 * values is a one-file change. Stage/product values are stored as displayed and are NOT translated.
 */
export const STAGE_OPTIONS: readonly string[] = [
  "Lead",
  "Erstkontakt",
  "Qualifiziert",
  "Angebot",
  "Gewonnen",
  "Verloren",
];

export const PRODUCT_OPTIONS: readonly string[] = ["Support & Care", "Digital Trust"];

export const STATUS_VALUES: readonly OpportunityStatus[] = ["OPEN", "WON", "LOST"];
