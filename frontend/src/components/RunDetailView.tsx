"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getValidationRun, listQuarantinedRecords, listValidationIssues, toUserMessage } from "@/lib/api";
import type { QuarantinedRecord, ValidationIssue, ValidationRun } from "@/lib/types";
import { RunSummary } from "@/components/RunSummary";
import { SeverityBadge } from "@/components/SeverityBadge";

interface RunDetailViewProps {
  runId: string;
}

export function RunDetailView({ runId }: RunDetailViewProps) {
  const [run, setRun] = useState<ValidationRun | null>(null);
  const [issues, setIssues] = useState<ValidationIssue[]>([]);
  const [records, setRecords] = useState<QuarantinedRecord[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    let isCurrent = true;

    Promise.all([getValidationRun(runId), listValidationIssues(runId), listQuarantinedRecords(runId)])
      .then(([runResponse, issueResponse, recordResponse]) => {
        if (isCurrent) {
          setRun(runResponse);
          setIssues(issueResponse.content);
          setRecords(recordResponse.content);
        }
      })
      .catch((loadError) => {
        if (isCurrent) {
          const message = toUserMessage(loadError);
          setNotFound(message.includes("not found"));
          setError(message);
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
  }, [runId]);

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6 lg:px-8">
      <Link className="w-fit rounded border border-line bg-white px-3 py-2 text-sm font-medium text-ink hover:bg-neutral-50" href="/">
        Back to runs
      </Link>
      {isLoading ? <PanelMessage message="Loading validation run..." /> : null}
      {!isLoading && notFound ? <PanelMessage tone="error" message={error ?? "Validation run not found."} /> : null}
      {!isLoading && error && !notFound ? <PanelMessage tone="error" message={error} /> : null}
      {!isLoading && run && !error ? (
        <>
          <RunSummary run={run} />
          <IssueTable issues={issues} />
          <QuarantinedRecords records={records} />
        </>
      ) : null}
    </main>
  );
}

function IssueTable({ issues }: { issues: ValidationIssue[] }) {
  return (
    <section className="rounded border border-line bg-white shadow-sm" aria-labelledby="issues-heading">
      <div className="border-b border-line px-5 py-4">
        <h2 id="issues-heading" className="text-lg font-semibold text-ink">
          Issues
        </h2>
      </div>
      {issues.length === 0 ? (
        <PanelMessage message="No issues recorded for this run." />
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-line text-left text-sm">
            <thead className="bg-neutral-50 text-xs uppercase text-neutral-500">
              <tr>
                <th className="px-5 py-3 font-semibold" scope="col">
                  Row
                </th>
                <th className="px-5 py-3 font-semibold" scope="col">
                  Code
                </th>
                <th className="px-5 py-3 font-semibold" scope="col">
                  Field
                </th>
                <th className="px-5 py-3 font-semibold" scope="col">
                  Severity
                </th>
                <th className="px-5 py-3 font-semibold" scope="col">
                  Message
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {issues.map((issue, index) => (
                <tr key={`${issue.code}-${issue.rowNumber ?? "file"}-${index}`}>
                  <td className="px-5 py-4 text-neutral-700">{issue.rowNumber ?? "File"}</td>
                  <td className="px-5 py-4 font-medium text-ink">{issue.code}</td>
                  <td className="px-5 py-4 text-neutral-700">{issue.fieldName ?? "N/A"}</td>
                  <td className="px-5 py-4">
                    <SeverityBadge severity={issue.severity} />
                  </td>
                  <td className="px-5 py-4 text-neutral-700">{issue.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function QuarantinedRecords({ records }: { records: QuarantinedRecord[] }) {
  return (
    <section className="rounded border border-line bg-white shadow-sm" aria-labelledby="quarantine-heading">
      <div className="border-b border-line px-5 py-4">
        <h2 id="quarantine-heading" className="text-lg font-semibold text-ink">
          Quarantined Records
        </h2>
      </div>
      {records.length === 0 ? (
        <PanelMessage message="No quarantined records for this run." />
      ) : (
        <div className="divide-y divide-line">
          {records.map((record) => (
            <article key={record.rowNumber} className="p-5">
              <div className="flex flex-wrap items-center gap-3">
                <h3 className="font-semibold text-ink">Row {record.rowNumber}</h3>
                <span className="text-sm text-neutral-500">{record.issueCodes.join(", ")}</span>
              </div>
              <pre className="mt-3 overflow-x-auto rounded border border-line bg-neutral-950 p-4 text-xs leading-6 text-neutral-100">
                {JSON.stringify(record.rawRecord, null, 2)}
              </pre>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function PanelMessage({ message, tone = "neutral" }: { message: string; tone?: "neutral" | "error" }) {
  const styles = tone === "error" ? "border-red-200 bg-red-50 text-red-700" : "border-line bg-white text-neutral-600";
  return <p className={`rounded border px-5 py-6 text-sm ${styles}`}>{message}</p>;
}
