import type { ValidationRun } from "@/lib/types";
import { OutcomeBadge } from "@/components/OutcomeBadge";
import { SeverityBadge } from "@/components/SeverityBadge";

interface RunSummaryProps {
  run: ValidationRun;
}

export function formatTimestamp(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function RunSummary({ run }: RunSummaryProps) {
  return (
    <article className="rounded border border-line bg-white p-4 shadow-sm">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="text-lg font-semibold text-ink">{run.originalFilename}</h2>
          <p className="mt-1 text-sm text-neutral-600">
            {run.dataSourceCode} · {run.schemaVersion}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <OutcomeBadge outcome={run.outcome} />
          <SeverityBadge severity={run.maxSeverity} />
        </div>
      </div>
      <dl className="mt-5 grid gap-3 sm:grid-cols-4">
        <Metric label="Total rows" value={run.counts.totalRows} />
        <Metric label="Valid rows" value={run.counts.validRows} />
        <Metric label="Invalid rows" value={run.counts.invalidRows} />
        <Metric label="Issues" value={run.counts.issueCount} />
      </dl>
      <p className="mt-4 text-sm text-neutral-600">Completed {formatTimestamp(run.completedAt)}</p>
    </article>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase text-neutral-500">{label}</dt>
      <dd className="mt-1 text-xl font-semibold text-ink">{value}</dd>
    </div>
  );
}
