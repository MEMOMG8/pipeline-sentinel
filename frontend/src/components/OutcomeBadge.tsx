import type { ValidationOutcome } from "@/lib/types";

interface OutcomeBadgeProps {
  outcome: ValidationOutcome;
}

export function OutcomeBadge({ outcome }: OutcomeBadgeProps) {
  const styles =
    outcome === "PASSED"
      ? "border-emerald-200 bg-emerald-50 text-emerald-700"
      : "border-red-200 bg-red-50 text-red-700";

  return <span className={`rounded border px-2 py-1 text-xs font-semibold ${styles}`}>{outcome}</span>;
}
