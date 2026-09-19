"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { submitValidationRun, toUserMessage } from "@/lib/api";

interface UploadCardProps {
  onSubmitted?: () => void;
}

export function UploadCard({ onSubmitted }: UploadCardProps) {
  const router = useRouter();
  const [file, setFile] = useState<File | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setMessage(null);

    if (!file) {
      setError("Choose a CSV file before submitting.");
      return;
    }

    setIsSubmitting(true);
    try {
      const run = await submitValidationRun(file);
      setMessage("Validation run saved. Opening run details...");
      onSubmitted?.();
      router.push(`/runs/${run.id}`);
    } catch (submitError) {
      setError(toUserMessage(submitError));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="rounded border border-line bg-white p-5 shadow-sm" aria-labelledby="upload-heading">
      <div className="flex flex-col gap-1">
        <p className="text-xs font-semibold uppercase text-neutral-500">Transaction Events · v1</p>
        <h2 id="upload-heading" className="text-lg font-semibold text-ink">
          Upload CSV
        </h2>
      </div>
      <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
        <div>
          <label className="block text-sm font-medium text-ink" htmlFor="validation-file">
            CSV file
          </label>
          <input
            id="validation-file"
            name="file"
            type="file"
            accept=".csv,text/csv"
            className="mt-2 block w-full rounded border border-line bg-white px-3 py-2 text-sm text-ink file:mr-4 file:rounded file:border-0 file:bg-neutral-100 file:px-3 file:py-2 file:text-sm file:font-medium file:text-ink hover:file:bg-neutral-200"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
        </div>
        <button
          type="submit"
          disabled={isSubmitting}
          className="inline-flex min-h-10 items-center rounded bg-ink px-4 py-2 text-sm font-semibold text-white transition hover:bg-neutral-700 disabled:cursor-not-allowed disabled:bg-neutral-400"
        >
          {isSubmitting ? "Submitting..." : "Submit validation run"}
        </button>
        {error ? (
          <p className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {error}
          </p>
        ) : null}
        {message ? (
          <p className="rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
            {message}
          </p>
        ) : null}
      </form>
    </section>
  );
}
