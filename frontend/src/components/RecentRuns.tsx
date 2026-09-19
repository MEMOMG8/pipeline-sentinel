"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { listValidationRuns, toUserMessage } from "@/lib/api";
import type { ValidationRun } from "@/lib/types";
import { OutcomeBadge } from "@/components/OutcomeBadge";
import { SeverityBadge } from "@/components/SeverityBadge";
import { formatTimestamp } from "@/components/RunSummary";

interface RecentRunsProps {
  refreshKey: number;
}

export function RecentRuns({ refreshKey }: RecentRunsProps) {
  const [runs, setRuns] = useState<ValidationRun[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isCurrent = true;

    listValidationRuns()
      .then((response) => {
        if (isCurrent) {
          setRuns(response.content);
        }
      })
      .catch((loadError) => {
        if (isCurrent) {
          setError(toUserMessage(loadError));
        }
      })
      .finally(() => {
        if (isCurrent) {
          setIsLoading(false);
        }
      });

    return () => {
      isCurrent = false;
    };
  }, [refreshKey]);

  return (
    <section className="rounded border border-line bg-white shadow-sm" aria-labelledby="recent-runs-heading">
      <div className="border-b border-line px-5 py-4">
        <h2 id="recent-runs-heading" className="text-lg font-semibold text-ink">
          Recent Validation Runs
        </h2>
      </div>
      {isLoading ? <StateMessage message="Loading recent runs..." /> : null}
      {error ? <StateMessage tone="error" message={error} /> : null}
      {!isLoading && !error && runs.length === 0 ? <StateMessage message="No validation runs yet." /> : null}
      {!isLoading && !error && runs.length > 0 ? (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-line text-left text-sm">
            <thead className="bg-neutral-50 text-xs uppercase text-neutral-500">
              <tr>
                <th className="px-5 py-3 font-semibold" scope="col">
                  File
                </th>
                <th className="px-5 py-3 font-semibold" scope="col">
                  Outcome
                </th>
                <th className="px-5 py-3 font-semibold" scope="col">
                  Severity
                </th>
                <th className="px-5 py-3 font-semibold" scope="col">
                  Rows
                </th>
                <th className="px-5 py-3 font-semibold" scope="col">
                  Completed
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {runs.map((run) => (
                <tr key={run.id} className="hover:bg-neutral-50">
                  <td className="px-5 py-4">
                    <Link className="font-medium text-ink underline-offset-4 hover:underline" href={`/runs/${run.id}`}>
                      {run.originalFilename}
                    </Link>
                    <p className="mt-1 text-xs text-neutral-500">{run.status}</p>
                  </td>
                  <td className="px-5 py-4">
                    <OutcomeBadge outcome={run.outcome} />
                  </td>
                  <td className="px-5 py-4">
                    <SeverityBadge severity={run.maxSeverity} />
                  </td>
                  <td className="px-5 py-4 text-neutral-700">
                    {run.counts.totalRows} total · {run.counts.invalidRows} invalid
                  </td>
                  <td className="px-5 py-4 text-neutral-700">{formatTimestamp(run.completedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}

function StateMessage({ message, tone = "neutral" }: { message: string; tone?: "neutral" | "error" }) {
  const styles = tone === "error" ? "text-red-700" : "text-neutral-600";
  return <p className={`px-5 py-6 text-sm ${styles}`}>{message}</p>;
}
