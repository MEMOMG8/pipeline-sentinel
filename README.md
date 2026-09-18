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

Milestone 2 still does not include frontend scaffolding, database setup, persistence, Flyway migrations, Docker Compose, AWS, authentication, stale-data validation, queues, notifications, AI features, or background workers.
