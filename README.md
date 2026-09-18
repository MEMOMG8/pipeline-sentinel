# Pipeline Sentinel

Pipeline Sentinel is planned as an internal data-quality and incident-triage platform. Milestone 1 is intentionally limited to backend foundation work.

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

Milestone 1 does not include frontend scaffolding, database setup, Flyway migrations, Docker Compose, AWS, authentication, ingestion, validation engines, queues, notifications, or AI features.
