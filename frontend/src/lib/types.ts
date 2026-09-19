export type ValidationRunStatus = "COMPLETED";

export type ValidationOutcome = "PASSED" | "REJECTED";

export type IssueSeverity = "MEDIUM" | "HIGH";

export type IssueCategory =
  | "SCHEMA"
  | "REQUIRED_VALUE"
  | "FORMAT"
  | "RANGE"
  | "DUPLICATE"
  | "PROCESSING";

export interface ValidationCounts {
  totalRows: number;
  validRows: number;
  invalidRows: number;
  issueCount: number;
}

export interface ValidationRun {
  id: string;
  dataSourceCode: string;
  schemaVersion: string;
  originalFilename: string;
  status: ValidationRunStatus;
  outcome: ValidationOutcome;
  maxSeverity: IssueSeverity | null;
  counts: ValidationCounts;
  submittedAt: string;
  completedAt: string;
}

export interface ValidationIssue {
  rowNumber: number | null;
  category: IssueCategory;
  code: string;
  fieldName: string | null;
  severity: IssueSeverity;
  message: string;
}

export interface QuarantinedRecord {
  rowNumber: number;
  rawRecord: Record<string, string>;
  issueCodes: string[];
}

export interface PagedResponse<T> {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  content: T[];
}

export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  path: string;
}
