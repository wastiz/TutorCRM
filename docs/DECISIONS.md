# Design decisions & spec reconciliations

This file records choices made where `CLAUDE.md` / `Report.md` were silent or inconsistent,
plus phase-by-phase progress.

## Confirmed with the product owner

| # | Question | Decision |
|---|----------|----------|
| 1 | Auth mechanism | Spring Security **OAuth2 Login** (Google) → backend issues its **own HS256 JWT** in an `HttpOnly` cookie (`TMS_SESSION`). Stateless. Uses `JWT_SECRET`. |
| 2 | `Student.studentNumber` (Номер уч.) origin | **Auto-assigned sequential per user**, starting at 1, stored as `String`. Gaps in `Report.md` examples are treated as archived/removed students. |
| 3 | Repo layout | Monorepo: `backend/` + `frontend/`, git initialised at root, two Railway services. |
| 4 | Sequencing | Full vertical (backend + frontend + tests) **phase by phase** per `CLAUDE.md` §62. |

## Spec inconsistencies and how they were resolved

| Area | Inconsistency | Resolution |
|------|---------------|------------|
| Spring Boot version | `CLAUDE.md` §4 says "Spring Boot 3.x"; Spring Initializr now only offers 4.x | Honour the spec — pinned **Spring Boot 3.5.9 on Java 21** (build via downloaded Temurin 21 + Gradle 8.14.3). |
| Student id field | §10.3 `personal_code`; `Report.md` §2 `isikukood` | Single field **`isikukood`** (`String`). |
| Student fields for reporting | `Report.md` §2 needs `studentNumber` + `userId` on `Student`; missing from §10.3 | Both added to the `Student` entity. |
| Import field count | §14 maps 16 fields (0–15); real examples in §57/§58 contain 17 tab fields (extra empty reserved slot before parent phone) | Parser is tolerant (§24): maps by position against the real examples, emits **warnings** instead of failing on count mismatch. Raw input is retained in the import DTO. |
| `parentSecondaryPhone` from import | No matching field on `Student` in §10.3 | `parentSecondaryPhone` (`String`) added to `Student`. |
| `Report` entity | §10.5 persists a `Report` row; `Report.md` computes on demand | Report is **computed on demand** from `Lesson`; a snapshot row is persisted only when the user exports to Sheets. |
| Cross-site cookie | Frontend + backend as separate Railway services breaks a `SameSite=Lax` cookie | Prod deploys the SPA behind nginx that proxies `/api`, `/oauth2`, `/login` to the backend → same origin. `JWT_COOKIE_SAMESITE` / `JWT_COOKIE_SECURE` are env-configurable for a split-domain setup (`None` + `Secure`). |
| CSRF | Cookie auth normally needs CSRF protection | CSRF disabled; mitigated by `SameSite` cookie + stateless JWT + explicit CORS allow-list. Acceptable for a single-user MVP; revisit if multi-tenant. |

## Google scopes

`openid`, `email`, `profile`,
`https://www.googleapis.com/auth/calendar`,
`https://www.googleapis.com/auth/spreadsheets`,
`https://www.googleapis.com/auth/drive.metadata.readonly` (needed to list existing spreadsheets the
user can access — `drive.file` would only see app-created files).

`access_type=offline` + `prompt=consent` are forced so Google returns a refresh token.

## Progress

- [x] **Phase 1 — Project setup**: Gradle Spring Boot 3.5.9 skeleton, Angular 22 SPA shell, Liquibase baseline, Dockerfiles, `docker-compose` (Postgres), CORS/error-handling infra, Testcontainers wiring.
- [x] **Phase 2 — Authentication**: Google OAuth2 login, `User` + `Authentication` slices, encrypted token storage, JWT cookie session, `/api/auth/me` + `/api/auth/logout`, Angular auth guard/interceptors/login screen.
- [ ] Phase 3 — Student (CRUD, import parser, preview, duplicate detection)
- [ ] Phase 4 — Lessons
- [ ] Phase 5 — Google Calendar
- [ ] Phase 6 — Reports
- [ ] Phase 7 — Google Sheets export
- [ ] Phase 8 — Dashboard
