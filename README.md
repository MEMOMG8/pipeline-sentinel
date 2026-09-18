# Pipeline Sentinel

Pipeline Sentinel is planned as an internal data-quality and incident-triage platform.

## Milestone 1

This milestone establishes a clean monorepo starting point and a minimal Spring Boot backend.

Current structure:

- `backend/` - Spring Boot 3 API service using Java 21 and Maven.
- `frontend/` - planned for a later milestone; not scaffolded yet.
- `docs/` - planned for later project documentation.
- `infrastructure/` - planned for later deployment and cloud work.

Implemented API:

- `GET /api/v1/health`

Example response:

```json
{
  "service": "pipeline-sentinel",
  "status": "UP"
}
```

## Prerequisites

- Java 21
- Maven 3.9.x, or the included `backend/mvnw.cmd` / `backend/mvnw` wrapper scripts

The wrapper scripts download Maven into `backend/.mvn/wrapper/` on first use.

## Run the Backend

From the repository root on Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

With Maven installed:

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Then call:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
```

## Run Tests

From `backend/`:

```powershell
.\mvnw.cmd test
```

## Build

From `backend/`:

```powershell
.\mvnw.cmd package
```

## Milestone 2

Milestone 2 adds an in-memory CSV validation preview for the `transaction-events` data source. Results are product previews only; they are not saved or persisted.

Endpoint:

- `POST /api/v1/validation-runs/preview`
- Content type: `multipart/form-data`
- Required file part: `file`
- Required form field: `dataSourceCode=transaction-events`

Supported CSV contract:

- Data source: `transaction-events`
- Schema version: `v1`
- Required headers: `transaction_id`, `customer_id`, `amount`, `currency`, `occurred_at`, `schema_version`

PowerShell example:

```powershell
@"
transaction_id,customer_id,amount,currency,occurred_at,schema_version
txn-001,cust-001,12.34,USD,2026-01-01T00:00:00Z,v1
txn-002,cust-002,56.78,USD,2026-01-02T00:00:00Z,v1
"@ | Set-Content -NoNewline .\transaction-events.csv

curl.exe -X POST http://localhost:8080/api/v1/validation-runs/preview `
  -F "dataSourceCode=transaction-events" `
  -F "file=@transaction-events.csv;type=text/csv"
```

Current validation rules:

- Uploaded filename must end in `.csv`, case-insensitive.
- File content must not be empty.
- Every required header must be present exactly once.
- Required fields must be nonblank after trimming whitespace.
- `amount` must parse as a decimal and be between `0.01` and `100000.00`, inclusive.
- `currency` must equal `USD`.
- `occurred_at` must be an ISO-8601 timestamp with zero UTC offset, such as `2026-01-01T00:00:00Z`.
- `schema_version` must equal `v1`.
- `transaction_id` must be unique within the uploaded file; later duplicates are rejected.

Data-quality failures return HTTP 200 with a preview result and issue list. Request problems such as unknown `dataSourceCode`, missing multipart fields, and unsupported file extensions return the JSON API error contract.

## Milestone 3

Milestone 3 adds PostgreSQL-backed audit persistence for completed validation runs. The existing preview endpoint remains in-memory and does not persist anything.

Persistence behavior:

- Flyway creates `data_sources`, `validation_runs`, `validation_issues`, and `quarantined_records`.
- Flyway seeds exactly one active data source: `transaction-events`, display name `Transaction Events`, schema version `v1`.
- `POST /api/v1/validation-runs` runs the same deterministic CSV validator as preview, saves the completed run, saves issues, and stores one quarantined record per invalid row.
- Invalid records are stored as JSONB using the uploaded CSV field values. Do not upload real sensitive data to this demo project.
- Whole-file disk quarantine is not implemented.

Preview versus persisted submission:

- `POST /api/v1/validation-runs/preview` returns a validation result only; no database write occurs.
- `POST /api/v1/validation-runs` persists the completed result and returns HTTP 201 with the run ID.

Local PostgreSQL setup:

Set these environment variables before running the backend with the `local` profile:

```powershell
$env:PIPELINE_SENTINEL_DB_URL = "jdbc:postgresql://localhost:5432/pipeline_sentinel"
$env:PIPELINE_SENTINEL_DB_USERNAME = "pipeline_sentinel"
$env:PIPELINE_SENTINEL_DB_PASSWORD = "change-me"
```

The same variable names are listed in `.env.example`. Do not commit real credentials.

Run the backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Create a persisted validation run:

```powershell
curl.exe -X POST http://localhost:8080/api/v1/validation-runs `
  -F "dataSourceCode=transaction-events" `
  -F "file=@transaction-events.csv;type=text/csv"
```

Retrieve persisted runs:

```powershell
curl.exe "http://localhost:8080/api/v1/validation-runs?page=0&size=20"
curl.exe "http://localhost:8080/api/v1/validation-runs/{runId}"
curl.exe "http://localhost:8080/api/v1/validation-runs/{runId}/issues?page=0&size=50"
curl.exe "http://localhost:8080/api/v1/validation-runs/{runId}/quarantined-records?page=0&size=50"
```

Milestone 3 still does not include frontend scaffolding, Docker Compose, whole-file disk quarantine, stale-data validation, data-source CRUD, AWS/cloud deployment, authentication, queues, notifications, AI features, CI, or background workers.
