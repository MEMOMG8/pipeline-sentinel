"use client";

import { useState } from "react";
import { RecentRuns } from "@/components/RecentRuns";
import { UploadCard } from "@/components/UploadCard";

export default function DashboardPage() {
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6 lg:px-8">
      <header className="border-b border-line pb-6">
        <p className="text-sm font-semibold uppercase text-neutral-500">Data quality dashboard</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-normal text-ink sm:text-4xl">Pipeline Sentinel</h1>
        <p className="mt-3 max-w-2xl text-base text-neutral-600">
          Validates batch data before downstream systems depend on it.
        </p>
      </header>
      <div className="grid gap-6 lg:grid-cols-[minmax(280px,360px)_1fr]">
        <UploadCard onSubmitted={() => setRefreshKey((value) => value + 1)} />
        <RecentRuns refreshKey={refreshKey} />
      </div>
    </main>
  );
}
