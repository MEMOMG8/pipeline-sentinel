import type { IssueSeverity } from "@/lib/types";

interface SeverityBadgeProps {
  severity: IssueSeverity | null;
}

export function SeverityBadge({ severity }: SeverityBadgeProps) {
  if (!severity) {
    return <span className="rounded border border-line px-2 py-1 text-xs font-medium text-neutral-500">None</span>;
  }

  const styles =
    severity === "HIGH"
      ? "border-red-200 bg-red-50 text-red-700"
      : "border-amber-200 bg-amber-50 text-amber-700";

  return <span className={`rounded border px-2 py-1 text-xs font-semibold ${styles}`}>{severity}</span>;
}
