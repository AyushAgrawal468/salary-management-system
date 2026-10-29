# Salary Management System — Requirements

## Goal
Replace ACME's Excel-based salary tracking for ~10,000 employees across multiple countries with a secure web application that lets HR **manage salary data** and **answer how the organisation pays people**.

## Users
| Role | Can do |
|---|---|
| `ADMIN` | Everything below + manage HR users, view audit log |
| `HR_MANAGER` | Create / edit / terminate employees, change salaries, import / export |
| `VIEWER` | Read-only access to employees and insights (e.g. finance, leadership) |

## In scope
1. **Authentication** — company SSO (OIDC) and local accounts (BCrypt, password policy, lockout after failed attempts, forced change of temporary password). Server-side sessions; secure, HttpOnly, SameSite cookies; CSRF protection.
2. **Employee directory** — server-side search, filter (country, department, job title, status), sort, pagination.
3. **Employee record** — employee code (unique), full name, job title, department, country, work email, phone, hire date, status (active / terminated). Terminate, never hard-delete.
4. **Salary history** — effective-dated salary entries in local currency; a raise adds a record, never overwrites one.
5. **Insights** — min / median / average / max salary by country, department and job title; headcount; pay distribution; outliers; cross-country comparison normalised via a maintained exchange-rate table.
6. **Bulk import (Excel/CSV)** with row-level validation and an error report; **CSV export**.
7. **Audit log** — every data change and login: who, when, before / after.
8. **Seed script** — 10,000 realistic employees.

## Non-functional requirements
- **Security:** role checks enforced on every API endpoint; HTTPS only; security headers (HSTS, CSP); secrets from environment, never in git; no personal data in logs.
- **Data protection:** encryption at rest (managed Postgres); field-level AES-GCM for email and phone with versioned keys for rotation; only fields with a clear business need are collected.
- **Integrity:** optimistic locking prevents lost concurrent edits; versioned schema migrations (Flyway).
- **Performance:** list and insight queries < 500 ms at 10k employees via indexes and SQL-side aggregation.
- **Quality:** TDD; unit tests + integration tests against real Postgres (Testcontainers); CI on every push.
- **Operability:** single Docker image; health checks; structured logs; consistent RFC 7807 error responses; OpenAPI docs.

## Deliberately out of scope
| Left out | Reason |
|---|---|
| Payroll runs, tax, payslips | Different product; HR's problem is *managing and analysing* pay data, not paying people |
| Employee self-service portal | Only persona is HR; widens attack surface on sensitive data |
| Approval workflows for salary changes | Audit log gives accountability now; workflows add process ACME hasn't asked for |
| Live FX rate feed | Rates table maintained by admins is enough for reporting; avoids a third-party dependency |
| Encrypting salary and name columns | Would block SQL search and aggregation — the core value. Protected by access control, audit log and at-rest encryption instead |
| Self-registration and email password reset | Accounts are admin-provisioned; avoids email infrastructure. SSO users reset via their IdP |
| Multi-tenancy, mobile app | Single organisation; responsive web UI covers tablets |

## Technical decisions
Java 21 + Spring Boot 3 · Spring Security · PostgreSQL + Flyway · Angular · single Docker image serving API + UI from one origin.
**Demo hosting:** free tiers (Render web service + Neon Postgres). Production would move to a paid tier with backups — no code change, same image.
