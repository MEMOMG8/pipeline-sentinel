import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { RunSummary } from "@/components/RunSummary";
import type { ValidationRun } from "@/lib/types";

const run: ValidationRun = {
  id: "11111111-1111-1111-1111-111111111111",
  dataSourceCode: "transaction-events",
  schemaVersion: "v1",
  originalFilename: "transactions.csv",
  status: "COMPLETED",
  outcome: "REJECTED",
  maxSeverity: "HIGH",
  counts: {
    totalRows: 12,
    validRows: 10,
    invalidRows: 2,
    issueCount: 3,
  },
  submittedAt: "2026-01-01T00:00:00Z",
  completedAt: "2026-01-01T00:00:02Z",
};

describe("RunSummary", () => {
  it("renders a validation run summary", () => {
    render(<RunSummary run={run} />);

    expect(screen.getByRole("heading", { name: "transactions.csv" })).toBeInTheDocument();
    expect(screen.getByText("transaction-events · v1")).toBeInTheDocument();
    expect(screen.getByText("REJECTED")).toBeInTheDocument();
    expect(screen.getByText("HIGH")).toBeInTheDocument();
    expect(screen.getByText("12")).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
  });
});
