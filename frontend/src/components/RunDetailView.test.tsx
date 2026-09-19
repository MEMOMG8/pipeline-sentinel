import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { RunDetailView } from "@/components/RunDetailView";
import { getValidationRun, listQuarantinedRecords, listValidationIssues } from "@/lib/api";
import type { PagedResponse, QuarantinedRecord, ValidationIssue, ValidationRun } from "@/lib/types";

vi.mock("@/lib/api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api")>("@/lib/api");
  return {
    ...actual,
    getValidationRun: vi.fn(),
    listValidationIssues: vi.fn(),
    listQuarantinedRecords: vi.fn(),
  };
});

const mockedGetValidationRun = vi.mocked(getValidationRun);
const mockedListValidationIssues = vi.mocked(listValidationIssues);
const mockedListQuarantinedRecords = vi.mocked(listQuarantinedRecords);

const run: ValidationRun = {
  id: "5329e083-0972-403c-9414-9b49c02a0b25",
  dataSourceCode: "transaction-events",
  schemaVersion: "v1",
  originalFilename: "invalid-events.csv",
  status: "COMPLETED",
  outcome: "REJECTED",
  maxSeverity: "HIGH",
  counts: {
    totalRows: 2,
    validRows: 1,
    invalidRows: 1,
    issueCount: 5,
  },
  submittedAt: "2026-01-01T00:00:00Z",
  completedAt: "2026-01-01T00:00:02Z",
};

const issues: PagedResponse<ValidationIssue> = {
  page: 0,
  size: 50,
  totalElements: 5,
  totalPages: 1,
  content: [
    {
      rowNumber: 3,
      category: "RANGE",
      code: "AMOUNT_OUT_OF_RANGE",
      fieldName: "amount",
      severity: "MEDIUM",
      message: "amount must be between 0.01 and 100000.00",
    },
    {
      rowNumber: 3,
      category: "FORMAT",
      code: "OCCURRED_AT_NOT_UTC",
      fieldName: "occurred_at",
      severity: "MEDIUM",
      message: "occurred_at must use UTC",
    },
    {
      rowNumber: 3,
      category: "FORMAT",
      code: "UNSUPPORTED_SCHEMA_VERSION",
      fieldName: "schema_version",
      severity: "HIGH",
      message: "schema_version must be v1",
    },
    {
      rowNumber: 3,
      category: "FORMAT",
      code: "UNSUPPORTED_CURRENCY",
      fieldName: "currency",
      severity: "MEDIUM",
      message: "currency must be USD",
    },
    {
      rowNumber: 3,
      category: "DUPLICATE",
      code: "DUPLICATE_TRANSACTION_ID",
      fieldName: "transaction_id",
      severity: "HIGH",
      message: "transaction_id must be unique",
    },
  ],
};

const quarantinedRecords: PagedResponse<QuarantinedRecord> = {
  page: 0,
  size: 50,
  totalElements: 1,
  totalPages: 1,
  content: [
    {
      rowNumber: 3,
      rawRecord: {
        transaction_id: "txn-001",
        customer_id: "cust-002",
        amount: "0.00",
        currency: "EUR",
        occurred_at: "2026-01-02T00:00:00-05:00",
        schema_version: "v2",
      },
      issueCodes: [
        "AMOUNT_OUT_OF_RANGE",
        "OCCURRED_AT_NOT_UTC",
        "UNSUPPORTED_SCHEMA_VERSION",
        "UNSUPPORTED_CURRENCY",
        "DUPLICATE_TRANSACTION_ID",
      ],
    },
  ],
};

describe("RunDetailView", () => {
  beforeEach(() => {
    mockedGetValidationRun.mockReset();
    mockedListValidationIssues.mockReset();
    mockedListQuarantinedRecords.mockReset();
  });

  it("renders the run summary, issues, and quarantined record response shapes", async () => {
    mockedGetValidationRun.mockResolvedValue(run);
    mockedListValidationIssues.mockResolvedValue(issues);
    mockedListQuarantinedRecords.mockResolvedValue(quarantinedRecords);

    render(<RunDetailView runId={run.id} />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "invalid-events.csv" })).toBeInTheDocument();
    });

    expect(mockedGetValidationRun).toHaveBeenCalledWith(run.id);
    expect(mockedListValidationIssues).toHaveBeenCalledWith(run.id);
    expect(mockedListQuarantinedRecords).toHaveBeenCalledWith(run.id);
    expect(screen.getAllByText("HIGH").length).toBeGreaterThan(0);
    expect(screen.getByText("AMOUNT_OUT_OF_RANGE")).toBeInTheDocument();
    expect(screen.getByText("DUPLICATE_TRANSACTION_ID")).toBeInTheDocument();
    expect(screen.getByText("Row 3")).toBeInTheDocument();
    expect(screen.getByText(/\"currency\": \"EUR\"/)).toBeInTheDocument();
    expect(screen.getByText(/\"schema_version\": \"v2\"/)).toBeInTheDocument();
  });

  it("renders an API error state when detail loading fails", async () => {
    mockedGetValidationRun.mockRejectedValue(new Error("Unexpected internal error"));
    mockedListValidationIssues.mockResolvedValue({ page: 0, size: 50, totalElements: 0, totalPages: 0, content: [] });
    mockedListQuarantinedRecords.mockResolvedValue({ page: 0, size: 50, totalElements: 0, totalPages: 0, content: [] });

    render(<RunDetailView runId={run.id} />);

    await waitFor(() => {
      expect(screen.getByText("Unexpected internal error")).toBeInTheDocument();
    });
  });
});
