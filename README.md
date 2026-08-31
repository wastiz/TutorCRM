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

## Build order

Implemented phase by phase per `CLAUDE.md` §62. Progress is tracked in `docs/DECISIONS.md`.
