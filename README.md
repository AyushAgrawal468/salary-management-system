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

## Run locally
```bash
# API on http://localhost:8080
cd backend && ./mvnw spring-boot:run

# UI on http://localhost:4200 (proxies /api to the API)
cd frontend && npm ci && npm start
```

## Test
```bash
cd backend && ./mvnw test
cd frontend && npm test -- --watch=false
```
