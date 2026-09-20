import type {
  ApiErrorResponse,
  PagedResponse,
  QuarantinedRecord,
  ValidationIssue,
  ValidationRun,
} from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081";

export class ApiClientError extends Error {
  readonly status: number;
  readonly details?: ApiErrorResponse;

  constructor(message: string, status: number, details?: ApiErrorResponse) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.details = details;
  }
}

function endpoint(path: string): string {
  return `${API_BASE_URL}${path}`;
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (!value || typeof value !== "object") {
    return false;
  }

  const candidate = value as Partial<ApiErrorResponse>;
  return (
    typeof candidate.status === "number" &&
    typeof candidate.error === "string" &&
    typeof candidate.message === "string" &&
    typeof candidate.path === "string"
  );
}

async function parseJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) {
    return null;
  }

  return JSON.parse(text) as unknown;
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(endpoint(path), {
    ...init,
    headers: {
      Accept: "application/json",
      ...init?.headers,
    },
  });
  const payload = await parseJson(response);

  if (!response.ok) {
    const details = isApiErrorResponse(payload) ? payload : undefined;
    throw new ApiClientError(details?.message ?? `Request failed with status ${response.status}`, response.status, details);
  }

  return payload as T;
}

export function listValidationRuns(): Promise<PagedResponse<ValidationRun>> {
  return requestJson<PagedResponse<ValidationRun>>("/api/v1/validation-runs?page=0&size=20");
}

export function getValidationRun(runId: string): Promise<ValidationRun> {
  return requestJson<ValidationRun>(`/api/v1/validation-runs/${runId}`);
}

export function listValidationIssues(runId: string): Promise<PagedResponse<ValidationIssue>> {
  return requestJson<PagedResponse<ValidationIssue>>(`/api/v1/validation-runs/${runId}/issues?page=0&size=50`);
}

export function listQuarantinedRecords(runId: string): Promise<PagedResponse<QuarantinedRecord>> {
  return requestJson<PagedResponse<QuarantinedRecord>>(
    `/api/v1/validation-runs/${runId}/quarantined-records?page=0&size=50`,
  );
}

export async function submitValidationRun(file: File): Promise<ValidationRun> {
  const formData = new FormData();
  formData.append("dataSourceCode", "transaction-events");
  formData.append("file", file);

  return requestJson<ValidationRun>("/api/v1/validation-runs", {
    method: "POST",
    body: formData,
  });
}

export function toUserMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Something went wrong while contacting the API.";
}
