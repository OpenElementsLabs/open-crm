import type { OpportunityStatus } from "@/lib/types";

/**
 * Status badge for opportunities: OPEN neutral, WON green (success token), LOST red (destructive
 * token) — consistent with the existing badge usage (spec 114). The label is passed in translated.
 */
export function OpportunityStatusBadge({
  status,
  label,
}: {
  readonly status: OpportunityStatus;
  readonly label: string;
}) {
  const className =
    status === "WON"
      ? "border-oe-green/30 bg-oe-green/10 text-oe-green"
      : status === "LOST"
        ? "border-oe-red/30 bg-oe-red/10 text-oe-red"
        : "border-oe-gray-light bg-oe-gray-light/30 text-oe-gray-mid";
  return (
    <span className={`inline-block rounded border px-2 py-0.5 text-xs ${className}`}>{label}</span>
  );
}

export function statusLabel(
  status: OpportunityStatus,
  labels: { readonly open: string; readonly won: string; readonly lost: string },
): string {
  switch (status) {
    case "WON":
      return labels.won;
    case "LOST":
      return labels.lost;
    default:
      return labels.open;
  }
}
