# Pipeline Sentinel

Pipeline Sentinel is a local data-quality and incident-triage demo. It validates uploaded batch CSV data before downstream systems consume it, stores an audit trail of validation runs, and lets an operator inspect issues and quarantined records in a small dashboard.

Current status: this is a local Docker demo. It is not cloud-deployed and does not include authentication, queues, object storage, background workers, observability, CI/CD, AI features, or production secrets management.

## Architecture

- `frontend/` - Next.js 16 App Router dashboard using TypeScript and Tailwind CSS.
- `backend/` - Java 21 / Spring Boot 3.3.5 API using Maven, Spring Web, Bean Validation, Spring Data JPA, Flyway, and PostgreSQL.
- `examples/` - synthetic CSV files for local demos.
- `compose.yaml` - local three-service demo stack: PostgreSQL, backend API, and frontend.

The persisted API validates the CSV with deterministic in-memory rules, saves completed validation runs to PostgreSQL, stores row-level issues, and stores one JSONB quarantined record per invalid row. The preview endpoint still returns an in-memory result and does not persist anything.

## Quick Demo

Prerequisites:

- Docker Desktop with Docker Compose

From the repository root:

```powershell
docker compose up --build
```

Open:

- Dashboard: `http://localhost:3000`
- API health: `http://localhost:8081/api/v1/health`

Browser demo flow:

1. Open `http://localhost:3000`.
2. Upload `examples/transaction-events-rejected-demo.csv`.
3. After submission, open the created run detail page.
4. Inspect the rejected run summary, five validation issues, and one quarantined record.

The demo CSV contains one valid row and one invalid row. The invalid row reliably produces these five issues: duplicate transaction ID, amount out of range, unsupported currency, non-UTC timestamp, and unsupported schema version.

Stop the stack:

```powershell
docker compose down
```

Reset the database volume and delete all local demo data:

```powershell
docker compose down -v
```

Warning: `docker compose down -v` removes the local PostgreSQL volume for this project.

## Local Ports

- Frontend: host `3000` -> container `3000`
- Backend API: host `8081` -> container `8080`
- PostgreSQL: host `5433` -> container `5432`

The backend is intentionally exposed on `8081` because Apache Airflow often uses host port `8080`.

## Environment

Safe local defaults are documented in `.env.example`.

For Docker Compose, the defaults work without creating a `.env` file. If you do create one, do not commit it.

Key variables:

```text
POSTGRES_DB=pipeline_sentinel
POSTGRES_USER=pipeline_sentinel
POSTGRES_PASSWORD=pipeline_sentinel_demo
PIPELINE_SENTINEL_CORS_ALLOWED_ORIGINS=http://localhost:3000
NEXT_PUBLIC_API_BASE_URL=http://localhost:8081
```

Do not upload real sensitive data. The current demo stores invalid row values as JSONB for audit visibility and does not yet implement whole-file disk quarantine.

## API Highlights

Health:

```powershell
Invoke-RestMethod http://localhost:8081/api/v1/health
```

Create a persisted validation run:

```powershell
curl.exe -X POST http://localhost:8081/api/v1/validation-runs `
  -F "dataSourceCode=transaction-events" `
  -F "file=@examples/transaction-events-rejected-demo.csv;type=text/csv"
```

Retrieve persisted results:

```powershell
curl.exe "http://localhost:8081/api/v1/validation-runs?page=0&size=20"
curl.exe "http://localhost:8081/api/v1/validation-runs/{runId}"
curl.exe "http://localhost:8081/api/v1/validation-runs/{runId}/issues?page=0&size=50"
curl.exe "http://localhost:8081/api/v1/validation-runs/{runId}/quarantined-records?page=0&size=50"
```

Preview without persistence:

```powershell
curl.exe -X POST http://localhost:8081/api/v1/validation-runs/preview `
  -F "dataSourceCode=transaction-events" `
  -F "file=@examples/transaction-events-rejected-demo.csv;type=text/csv"
```

## Validation Rules

Supported CSV contract:

- Data source: `transaction-events`
- Schema version: `v1`
- Required headers: `transaction_id`, `customer_id`, `amount`, `currency`, `occurred_at`, `schema_version`

Current rules:

- Uploaded filename must end in `.csv`, case-insensitive.
- File content must not be empty.
- Every required header must be present exactly once.
- Required fields must be nonblank after trimming whitespace.
- `amount` must parse as a decimal and be between `0.01` and `100000.00`, inclusive.
- `currency` must equal `USD`.
- `occurred_at` must be an ISO-8601 timestamp with zero UTC offset, such as `2026-01-01T00:00:00Z`.
- `schema_version` must equal `v1`.
- `transaction_id` must be unique within the uploaded file; later duplicates are rejected.

Data-quality failures return HTTP 200 for preview and a persisted rejected run for submission. Request problems such as unknown `dataSourceCode`, missing multipart fields, and unsupported file extensions return the JSON API error contract.

## Manual Development

Docker Compose is the recommended demo path. Use the commands below when you want to run services manually during development.

### Backend

Prerequisites:

- Java 21
- Maven 3.9.x, or the included `backend/mvnw.cmd` / `backend/mvnw` wrapper scripts
- PostgreSQL 16

Run PostgreSQL manually on host port `5433`:

```powershell
docker run --rm --name pipeline-sentinel-postgres `
  -e POSTGRES_DB=pipeline_sentinel `
  -e POSTGRES_USER=pipeline_sentinel `
  -e POSTGRES_PASSWORD=pipeline_sentinel_demo `
  -p 5433:5432 `
  postgres:16-alpine
```

Run the backend:

```powershell
cd backend
$env:PIPELINE_SENTINEL_DB_URL = "jdbc:postgresql://localhost:5433/pipeline_sentinel"
$env:PIPELINE_SENTINEL_DB_USERNAME = "pipeline_sentinel"
$env:PIPELINE_SENTINEL_DB_PASSWORD = "pipeline_sentinel_demo"
$env:PIPELINE_SENTINEL_CORS_ALLOWED_ORIGINS = "http://localhost:3000"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Backend quality commands:

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd package
```

### Frontend

Prerequisites:

- Node.js 20+
- npm 11+

Create `frontend/.env.local`:

```powershell
NEXT_PUBLIC_API_BASE_URL=http://localhost:8081
```

Run the frontend:

```powershell
cd frontend
npm install
npm run dev
```

Frontend quality commands:

```powershell
cd frontend
npm run test
npm run lint
npm run build
```

## Implemented Milestones

- Milestone 1: Spring Boot foundation, health endpoint, API error contract, local/test profiles.
- Milestone 2: in-memory CSV validation preview at `POST /api/v1/validation-runs/preview`.
- Milestone 3: PostgreSQL/Flyway audit persistence for completed validation runs.
- Milestone 4: local Next.js dashboard for upload, recent runs, run detail, issues, and quarantined records.
- Milestone 5: reproducible local Docker Compose demo and recruiter-ready documentation.

Explicit current limitations: no whole-file disk quarantine, no freshness validation, no frontend auth, no cloud deployment, no Docker Compose production hardening, no queues, no notifications, no charts, no observability, and no AI features.
