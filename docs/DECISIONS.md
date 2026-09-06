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
| Import package name | §17 shows `student/import/` | `import` is a Java reserved word — the package is `student/importer/`. |
| `POST /api/students/import/create` | §49 defines it separately | Implemented; delegates to the same create path as `POST /api/students` (validation + duplicate detection included). |
| Duplicate detection response | §26 shows a UI choice, no wire format given | `POST /api/students` returns **409 `POSSIBLE_DUPLICATE`** with `details.duplicates: [StudentSummary]`; `ignoreDuplicates:true` in the body overrides. A `POST /api/students/duplicates` lookup backs the pre-submit check. |
| Student archival | §48 has `DELETE`; §39/§60 say "archive" | `DELETE` hard-deletes **only when the student has no lessons** (else 409 `STUDENT_HAS_LESSONS`); separate `POST /api/students/{id}/archive` sets status `FINISHED` + `endDate`. |
| Lesson list filtering | §50 lists `GET /api/lessons` with no filter spec | Optional `studentId`, `status`, `from`, `to` (ISO date-time) query params; filtered in the service (per-tutor lesson volume is small). |
| Recurring lessons | §36 describes the flow, no endpoint | `POST /api/lessons/{id}/repeat` `{occurrences, intervalWeeks?}` clones the source lesson weekly; each copy is its own record (own calendar event later). |
| Cross-site cookie | Frontend + backend as separate Railway services breaks a `SameSite=Lax` cookie | Prod deploys the SPA behind nginx that proxies `/api`, `/oauth2`, `/login` to the backend → same origin. `JWT_COOKIE_SAMESITE` / `JWT_COOKIE_SECURE` are env-configurable for a split-domain setup (`None` + `Secure`). |
| CSRF | Cookie auth normally needs CSRF protection | CSRF disabled; mitigated by `SameSite` cookie + stateless JWT + explicit CORS allow-list. Acceptable for a single-user MVP; revisit if multi-tenant. |

## More resolutions (Phase 5)

| Area | Decision |
|------|----------|
| Where "which calendar / spreadsheet" is stored | New `settings` slice: `user_settings` table, `GET/PUT /api/settings`. |
| Lesson ↔ Calendar coupling | Lesson slice defines a `LessonCalendarGateway` port; the Google adapter lives in `integration/google/calendar` and is the only place that imports Google SDK classes (CLAUDE.md §63). |
| `CalendarSyncStatus` when Google is not connected / no calendar chosen | `DISABLED` (not `PENDING` — there is no background sync worker in the MVP). `FAILED` is set only when a real push attempt errors; the user retries via `POST /api/lessons/{id}/sync-calendar`. |
| Token storage vs Spring's `OAuth2AuthorizedClientService` | We persist the encrypted refresh token in our `authentication` table and rebuild `UserCredentials` per call; rotated access tokens are written back. |

## More resolutions (Phase 6)

| Area | Decision |
|------|----------|
| Report month boundary timezone | Lessons are `timestamptz` but we don't store a per-tutor timezone; the report window is `[month.atDay(1) 00:00Z, nextMonth.atDay(1) 00:00Z)`. Good enough for a single-tutor MVP; revisit if tutors span timezones. |
| `StudentReportRowDto.lessonPrice` when a student had several prices in the month | `null` (+ a warning). `total` is always the true sum of the individual `Lesson.price` values, so the month total stays correct. |
| `errors` vs `warnings` | `errors` block export (Report.md §22); the only one that can currently fire is "a completed lesson references a student that no longer exists" (shouldn't happen — `lesson.student_id` FK is `RESTRICT`). `warnings` (multi-price) allow export after the user confirms. |
| `Report` entity | Persisted only as an **export snapshot** (Phase 7); the Reports page always shows freshly computed figures. |

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
- [x] **Phase 3 — Student**: `student` + `student/schedule` + `student/importer` slices; CRUD API, per-user sequential `studentNumber`, `StudentSchedule` rows, deterministic `TabStudentImportParser` (17 unit tests incl. both real examples), `/import/parse` + `/import/create`, duplicate detection; Angular students list (search/filter), shared reactive form, create screen with Manual / Import-from-message tabs + warnings + preview, edit, detail, duplicate + confirm dialogs. Backend 38 tests green.
- [x] **Phase 4 — Lessons**: `lesson` slice — `Lesson` entity (frozen per-lesson `price`, `CalendarSyncStatus`), `LessonStatus`, CRUD API, `complete`/`cancel`/`no-show`, `POST /{id}/repeat` weekly generation (CLAUDE.md §36), `GET /student/{id}/overview` (upcoming/past/this-month/earnings), `LessonQueryService` feeds `nextLessonAt` into the students list and blocks deleting a student with lessons. Liquibase 0003. Angular: lesson create/edit dialog (with inline "repeat"), FullCalendar calendar page (month/week/day, click-to-create, status colours), student detail lessons section. `sync-calendar` endpoint deferred to Phase 5. Backend 46 tests green.
- [x] **Phase 5 — Google Calendar**: `integration/google/**` — `GoogleApiFactory` (auto-refreshing `UserCredentials`, persists rotated access token), `GoogleCalendarService` (list/create/update/delete events, extended properties `application=tutor-management` + `lessonId`), `GoogleErrors` (403→friendly, etc.), `GoogleCalendarLessonAdapter` implements the lesson slice's `LessonCalendarGateway` port. `settings` slice (`UserSettings` + `/api/settings`) stores the chosen calendar. `LessonService` mirrors create/update/status/delete to Calendar, marking `calendarSyncStatus` SYNCED/FAILED/DISABLED — a lesson is never lost on Google failure (§34). `POST /api/lessons/{id}/sync-calendar` retry. `GET /api/integrations/google/{status,calendars}`, `POST .../disconnect`. Angular Settings page (connect/reconnect/disconnect, calendar picker), sync badges + retry on student detail. Liquibase 0004. Live Google calls need real OAuth creds — untestable here; layer is isolated + unit-safe (46 backend tests green).
- [x] **Phase 6 — Reports**: `report` slice — `ReportService.generate(userId, YearMonth)` computes the monthly payout report purely from `COMPLETED` lessons in `[firstOfMonth, firstOfNextMonth)` UTC (Report.md §8), grouped by student, **price from `Lesson.price`** not the current student price (§9), sorted by `studentNumber` ASC (§20), Russian month title "август 26" (§3), multi-price → warning not blocker (§11, §22), missing email/parent → empty cell (§21). `GET /api/reports/monthly?month=yyyy-MM` → `MonthlyReportDto`. Persisted `Report` snapshot entity + Liquibase 0005 (written on export, Phase 7). Angular Reports page: month picker, Report.md column layout with ИТОГО footer row, warning/error banners, Export button (wired Phase 7). 7 `ReportServiceIT` tests — 53 backend tests green.
- [x] **Phase 7 — Google Sheets export**: `report/export/ReportSheetGrid` (pure transform → the tutor's exact A–J layout: title row, headers, student rows, total row with G=count / J=amount per Report.md §5, §15); `MonthlyReportSheetExporter` port + `GoogleSheetsReportExporter` adapter (integration side); `GoogleSheetsService` (Drive `files.list` for spreadsheets, Sheets `get`/`values.update`/`values.clear`/`batchUpdate` for per-month worksheet create + bold/border/number-format/auto-resize per §16). `POST /api/reports/monthly/export {month, overwrite}` → validates `exportable`, requires a configured spreadsheet, per-month worksheet titled "август 26" unless a fixed title is set, `409 WORKSHEET_EXISTS` when it exists and `overwrite=false` (§18), persists a `Report` snapshot. `GET /api/integrations/google/spreadsheets[/{id}]`. Angular: Settings spreadsheet + worksheet pickers, Reports export button with the "Update existing / Cancel" confirm. `ReportSheetGridTest` + `ReportServiceExportTest` (mocked) — 57 backend tests green. Live Sheets writes need real OAuth creds.
- [x] **Phase 8 — Dashboard**: `dashboard` slice — `GET /api/dashboard` → active students, lessons today / this week, completed this month, earned-so-far vs expected earnings (COMPLETED + PLANNED this month), next 6 upcoming lessons. Angular dashboard with stat tiles + upcoming list. `DashboardServiceIT`. 59 backend tests green.

## Post-MVP: Google Calendar → app import (owner request, 2026-08-31)

`CLAUDE.md` §61 lists "two-way Google Calendar synchronization" as out of MVP scope. The owner
asked for the pull direction (create events in your own Google Calendar, app imports them as
lessons), keeping in-app creation. Added, review-then-confirm style (like the message import):

- **Bug fixed:** the Calendar page fetched `/api/lessons?from=2026-08-31` (date-only, from
  FullCalendar) and the backend only accepted a full date-time → 500 → nothing rendered.
  Frontend now sends `info.start.toISOString()`; backend `TimeParams` accepts date **or**
  date-time on `from`/`to`.
- `CalendarEventSource` port (lesson slice) + `GoogleCalendarEventSourceAdapter` (integration).
  `GoogleCalendarService.listEvents` (recurrences expanded, all-day skipped) + `linkEventToLesson`
  (writes `application`/`lessonId` private props back onto a user-created event).
- `LessonImportService`: `preview(from,to)` classifies events into **new** (guess a student from
  the title / attendee email — single unambiguous match only), **moved** (a linked lesson whose
  event time changed in Google), and **already-linked**. `importSelected` creates `PLANNED`
  lessons linked to the events (`calendarSyncStatus = SYNCED`), tags each event, and optionally
  pulls time changes for moved lessons.
- `GET /api/lessons/google/preview`, `POST /api/lessons/google/import`.
- Calendar page: **"Sync from Google"** button with a badge counting importable events in the
  visible range; dialog to pick/confirm students and prices.
- 4 `LessonImportServiceIT` tests (fake `CalendarEventSource`) — 63 backend tests green.
  Verified live against the owner's real calendar: preview returns events, student-match guessing,
  moved detection, and dedup of already-linked events all work; no live mutation performed.

## End-to-end verification (2026-08-31)

Ran against local Postgres with a minted dev session cookie:
parse raw message → create student (#1) via `/import/create` → create first lesson (Aug 2026) →
`/repeat` ×3 (4 Thursdays) → mark all COMPLETED → `GET /reports/monthly?month=2026-08`
returns title "август 26", 4 lessons, €80 → `GET /dashboard` reflects the same.
Covers Definition of Done items 2–12. Items 1/7/13/14 (Google login, Calendar event visible,
pick Sheet, export) need real Google OAuth credentials; the integration layer is built and
returns friendly errors without them. Item 15 (Railway) needs a Railway account — Dockerfiles +
`railway.json` + `docs/RAILWAY.md` are ready.

## Post-MVP: smarter student import (owner request, 2026-09-05)

Task: recognize isikukood, phone, first/last name, e-mail and the remaining fields out of free
text, tolerating missing separators.

| Area | Decision |
|------|----------|
| Recognition vs position | Positions now drive only the fields that cannot be told apart by content (school, subject, goal, grade, lessons per week). Identity fields are recognized by shape anywhere in the message: `ContactScanner` (e-mail / isikukood / phone) + `ImportValues` (name, days, time range, lesson format). |
| **Spec correction** | `CLAUDE.md` §58 calls the last two fields "Parent phone / Parent secondary phone". They are not phones: `51109180029` and `47808060232` are valid isikukoods (checksum verified), and the first decodes to 2011‑09‑18 — exactly the age 14 stated in the same message, i.e. the **student's own** code; the second (1978‑08‑06) is the parent's. Estonian phone numbers are 7–8 digits. The parser now maps column 15 → `isikukood` and column 16 → `parentIsikukood`; `parentPhone` stays free for a real phone number. |
| New student fields | `parentIsikukood` and `telegram` (Liquibase `0006`). Telegram is filled by the parser here; the clickable link is part of the Telegram task. |
| Glued values | `ljulap@gmail.com5350 6894` (the e-mail regex stops the TLD at the first non-letter), `5350689451109180029` (an 11-digit isikukood is peeled off either end when the remainder is a plausible phone), `AnnaIvanova` (camel-case name split). A name glued to an e-mail with no case boundary (`Tsarenkovljulap@…`) stays ambiguous and is left to the Preview step. |
| Two personal codes in one message | The younger birth date is the student; an explicitly stated age wins over that guess (±1 year). |
| Student vs parent side | Everything from the parent-name column (or the first "мама / родитель / parent…" word in free text) onwards is treated as the parent's contact data. |
| Free text | `FreeTextStudentImportParser` handles prose, one-value-per-line and separator-less pastes; whatever it cannot classify becomes `goal`, so nothing is lost (§24). `SmartStudentImportParser` (`@Primary`) routes by shape and, for a half-broken paste, keeps whichever strategy recognized more identity fields. Still deterministic — no LLM (§25). |
| Age from isikukood | When the message has no age, it is derived from the personal code and reported as a warning in the Preview. |
| `StudentRequest` builder | Adding fields kept breaking the positional record constructor in tests; `StudentRequest` and `ImportedStudentDto` now carry `@Builder` and the tests use it. |

New tests: `IsikukoodTest`, `FreeTextStudentImportParserTest`, `SmartStudentImportParserTest`,
plus isikukood/glued-value cases in `TabStudentImportParserTest` — 80 backend tests green.

## Post-MVP batch (owner request, 2026-09-05)

Ten follow-up tasks. Decisions confirmed with the owner up front: e-mail goes out through the
**Gmail API on the existing Google OAuth** and is **sent manually only** (no automatic triggers);
a lesson that did not happen is **deleted from Google Calendar** and hidden in the app calendar;
the reason a student left is **free text**.

| # | Task | Decision |
|---|------|----------|
| 2 | Required student fields | Only `firstName`, `lastName`, `email` (`@NotBlank @Email`). Applies to update as well as create — a student without an e-mail cannot be saved from the form any more. An unfinished schedule row no longer blocks the form; it is dropped on submit. |
| 3 | One rate, several lengths | `Student.lessonPrice` is re-read as **the rate for a standard 60-minute lesson** (no rename, no migration); `LessonPricing` computes `rate × duration/60` rounded to cents. Update reprices only when the duration or the student changed — editing notes never rewrites a historical price (§10.4). The dialog offers 1 / 1.5 / 2 h (plus the lesson's own length if it differs) and fills the price in read-only, with a "Custom price" override that is switched on automatically for an existing hand-priced lesson. |
| 4 | Dashboard for today | `DashboardDto.todaysLessonList`; marking a lesson `CANCELLED`/`NO_SHOW` deletes its calendar event, clears the event id and sets the new `CalendarSyncStatus.REMOVED` — the lesson row survives for the report and the statistics. `POST /api/lessons/{id}/replan` undoes it and re-creates the event. The app calendar hides such lessons behind a "Show cancelled" toggle. |
| 5 | E-mail templates | Stored per tutor in `email_template` (Liquibase 0007) and edited in Settings, so no deploy is needed to change wording; three starter templates are seeded on first use. `{{placeholders}}` are rendered by the pure `EmailTemplateRenderer`; an unresolved one is left visible and reported instead of being sent blank. `EmailSender` port + `GmailEmailSender` adapter; the MIME message is built by hand (`MimeMessages`) to avoid an SMTP stack — UTF-8 body, RFC 2047 subject, header-injection guard. **New scope `gmail.send` — Google must be reconnected once.** |
| 6 | Calendar sync bugs | Three real defects: (a) `timeZone` was set from `OffsetDateTime.getOffset().getId()` — `"Z"` / `"+03:00"` are not IANA zone ids and Google rejects them; the field is now omitted since the RFC 3339 value already carries the offset. (b) `updateEvent` treated **any** `IOException` from the pre-fetch as "event is gone" and created a duplicate; only 404/410 re-create now. (c) updating an event replaced all private extended properties; only our two keys are written. Plus `POST /api/lessons/student/{id}/sync-calendar` for one student, and the event now shows the **duration** instead of the price (`price` was removed from `LessonSyncCommand` altogether — a shared calendar should not leak earnings). |
| 7 | Telegram | `Student.telegram` (Liquibase 0006), normalized to a bare username whatever the tutor pastes (`@name`, `t.me/name`, full URL); the import parser recognizes handles in free text. Clickable in the list and on the student page. |
| 8 | Responsive | `BreakpointObserver` switches the shell drawer to `over` on handsets; tables scroll inside `.scroll-x` and drop secondary columns by CSS; the calendar starts in day view on a phone; dialogs and forms collapse to one column. |
| 9 | File split | Every component now uses `templateUrl` / `styleUrl` — no inline templates or styles are left in the codebase. |
| 10 | Statistics | New `statistics` slice, `GET /api/statistics`: student counts by status, lesson counts by status with a "fell through" rate, all-time earnings, a 12-month breakdown, and the free-text leave reasons. `Student.leaveReason` (Liquibase 0008) is asked for in a dialog when archiving. |

Backend 114 tests green; frontend builds and its Vitest suite passes.

**Not verified here:** everything that needs real Google credentials (Gmail send, live calendar
writes) and on-device checks of the responsive layout.

## Dev seed data (owner request, 2026-09-06)

Goal: after every update, open the app and click through real-looking data without a Google account.

| Area | Decision |
|------|----------|
| Where it lives | New `dev/` package (infrastructure, next to `config/` and `security/` per CLAUDE.md §9): `DevProperties`, `DevDataSeeder`, `DevDataRunner`, `DevAuthController`, `DevSecurityConfig`. |
| How it is gated | Every bean carries `@Profile("local")`, so in a deployed environment the seeder and its routes are **not registered at all** — `DevEndpointsDisabledIT` asserts exactly that under the default profile. No environment variable can switch them on. |
| Ownership of the data | Everything hangs off one demo tutor identified by the fake Google subject `dev-local-seed`. A reseed only ever deletes that tutor's rows, so a database that also holds a real Google account is safe. |
| Idempotency | Startup seeding fills an **empty** demo account only, so a restart after a code change keeps whatever you clicked together. `app.dev.reset-on-startup=true` (or `POST /dev/seed?reset=true`) wipes and regenerates with fresh dates. |
| Signing in | `GET /dev/login` issues the normal session cookie for the demo tutor and redirects to the SPA callback — the same cookie the Google flow issues, so nothing else in the app behaves differently. The login screen shows a "Dev sign-in" button when `environment.production` is false, and `proxy.conf.json` forwards `/dev` so the cookie lands on the `ng serve` origin. |
| Cookie duplication | `SessionCookies` was extracted from `OAuth2LoginSuccessHandler`; the Google login, the logout endpoint and the dev sign-in now share one definition of the session cookie. |
| What the data covers | 8 students (5 active / 1 paused / 2 archived **with free-text leave reasons**), one deliberately holding only the three required fields; valid isikukoods for students and parents, telegram handles, parent contacts, weekly schedules; ~107 lessons over 18 weeks back and 3 weeks ahead in every status, of 60 / 90 / 120 minutes so prices exercise the rate × duration rule; **three lessons today** so the dashboard always has something to mark off; the first student's rate rises mid-month so the monthly report raises its multi-price warning; the three starter e-mail templates are materialised. |
| Google-dependent state | Lessons are seeded as `calendarSyncStatus = DISABLED` — the demo tutor has no Google tokens, and a fake `SYNCED` would be misleading. |

Verified locally end to end: `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun` seeds 8 students /
107 lessons, `/dev/login` (directly and through the `ng serve` proxy) returns a working session,
and `/api/dashboard`, `/api/statistics` and `/api/reports/monthly` all return populated data
including the multi-price warning.
