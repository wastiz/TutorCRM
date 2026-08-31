# Tutor Management System

Web app that replaces a tutor's manual bookkeeping of students, lessons and monthly
payout reporting, integrating with **Google Calendar** (scheduling) and **Google Sheets**
(reporting). PostgreSQL is the single source of truth.

Full functional spec: [`CLAUDE.md`](./CLAUDE.md) and [`Report.md`](./Report.md).
Design decisions & spec reconciliations: [`docs/DECISIONS.md`](./docs/DECISIONS.md).

## Stack

| Layer      | Tech |
|------------|------|
| Backend    | Java 21, Spring Boot 3.5, Spring Web / Security / Data JPA / Validation / OAuth2 Client / Actuator, PostgreSQL, Liquibase, MapStruct, Lombok, Gradle |
| Frontend   | Angular 22 (standalone, zoneless, signals), Angular Material, RxJS, FullCalendar |
| Infra      | Docker, Railway, PostgreSQL |

## Repository layout

```
backend/    Spring Boot app — vertical slice per entity (user, authentication, student, lesson, report)
frontend/   Angular SPA
docs/       Design decisions, Railway deployment notes
docker-compose.yml   Local PostgreSQL
```

## Local development

Prerequisites: JDK 21, Node 24.15+, Docker.

```bash
# 1. Database
docker compose up -d db

# 2. Backend  (http://localhost:8080)
cd backend
cp src/main/resources/application-local.example.yml src/main/resources/application-local.yml   # fill in Google client id/secret
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun

# 3. Frontend (http://localhost:4200, proxies /api -> :8080)
cd frontend
npm install
npm start
```

### Google OAuth setup

Create an OAuth 2.0 Client (type: Web application) in Google Cloud Console with:

- Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
- Enabled APIs: Google Calendar API, Google Sheets API, Google Drive API

Scopes requested: `openid email profile`,
`.../auth/calendar`, `.../auth/spreadsheets`, `.../auth/drive.metadata.readonly`.

## Tests

```bash
cd backend  && ./gradlew test      # JUnit 5 + Testcontainers PostgreSQL
cd frontend && npm test            # Vitest
```

## Status

All 8 phases from `CLAUDE.md` §62 are implemented:

| Phase | Scope |
|-------|-------|
| 1 | Project setup — Gradle/Spring Boot 3.5, Angular 22, Liquibase, Docker, Railway config |
| 2 | Google OAuth2 login → app JWT cookie session, `user` + `authentication` slices |
| 3 | Student CRUD, `StudentSchedule`, deterministic raw-message import parser + preview, duplicate detection |
| 4 | Lesson CRUD, status flow, weekly recurring, FullCalendar page, per-student overview |
| 5 | Google Calendar integration — event create/update/delete, sync-status fallback, settings |
| 6 | Monthly report computed from `Lesson` rows (Report.md) |
| 7 | Export the report to an existing Google Sheet (per-month worksheet, formatting) |
| 8 | Dashboard aggregates |

Backend: 59 tests (JUnit 5 + Testcontainers). Frontend builds + Vitest green.
End-to-end flow (import → student → lessons → report → dashboard) verified locally;
see `docs/DECISIONS.md` for the full decision log and what still needs real Google
credentials / a Railway account.
