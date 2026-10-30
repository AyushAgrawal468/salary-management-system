# Salary Management System

Web application for HR to manage and analyse salaries of ~10,000 employees across countries.
See [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) for scope and decisions.

| Part | Stack | Folder |
|---|---|---|
| API | Java 21, Spring Boot 4 | `backend/` |
| UI | Angular 21 | `frontend/` |

## Prerequisites
- JDK 21
- Node.js 22.12+ (npm)
- PostgreSQL (tested on 18) running on `localhost:5432`

## Database setup (once)
```bash
psql -U postgres -f backend/scripts/create-local-db.sql
```
Creates the `salary_app` login plus `salary` (app) and `salary_test` (tests; wiped on every test run).
Override with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (and `TEST_DB_URL` for tests).

## Run locally
```bash
# API on http://localhost:8080
cd backend && ./mvnw spring-boot:run

# UI on http://localhost:4200 (proxies /api to the API)
cd frontend && npm ci && npm start
```

## First login
All `/api/**` endpoints require a session. On a database with no users, the app creates the first
`ADMIN` from two environment variables, then must be logged into once to replace that password:
```bash
cd backend && APP_ADMIN_USERNAME=admin APP_ADMIN_PASSWORD='<temporary password>' ./mvnw spring-boot:run
```
Roles: `ADMIN` (everything), `HR_MANAGER` (edit employees and salaries), `VIEWER` (read-only).
Five wrong passwords lock an account for 15 minutes.

## Demo data
```bash
# Inserts 10,000 employees (~54k salary entries, 8 countries) on startup; skipped if any employees exist.
cd backend && APP_SEED_ENABLED=true ./mvnw spring-boot:run
```
`APP_SEED_EMPLOYEES` changes the count. Seeding is off unless explicitly enabled. Data is generated
from a fixed random seed, so names, pay bands and histories are the same on every machine.

## Test
```bash
cd backend && ./mvnw test
cd frontend && npm test -- --watch=false
```
