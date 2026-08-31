# Railway deployment

Three components in one Railway project:

```
Railway project
├── Postgres    (Railway PostgreSQL plugin)
├── backend     (root: /backend, Dockerfile build)
└── frontend    (root: /frontend, Dockerfile build)
```

## 1. Postgres

Add the **PostgreSQL** plugin. It exposes `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`.
Liquibase migrations run automatically on backend start.

## 2. backend service

- **Root directory:** `backend`
- **Builder:** Dockerfile (`backend/railway.json` sets this)
- **Variables:**

| Variable | Value |
|----------|-------|
| `DATABASE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DATABASE_USERNAME` | `${{Postgres.PGUSER}}` |
| `DATABASE_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `GOOGLE_CLIENT_ID` | from Google Cloud Console |
| `GOOGLE_CLIENT_SECRET` | from Google Cloud Console |
| `APP_API_BASE_URL` | `https://<backend-domain>` (or the shared frontend domain if proxied) |
| `APP_WEB_BASE_URL` | `https://<frontend-domain>` |
| `APP_CORS_ALLOWED_ORIGINS` | `https://<frontend-domain>` |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `TOKEN_ENCRYPTION_KEY` | `openssl rand -base64 32` |
| `JWT_COOKIE_SECURE` | `true` |
| `JWT_COOKIE_SAMESITE` | `Lax` if the SPA proxies to the backend (recommended); `None` if split-domain |

## 3. frontend service

- **Root directory:** `frontend`
- **Builder:** Dockerfile
- **Variables:**

| Variable | Value |
|----------|-------|
| `BACKEND_URL` | `http://backend.railway.internal:8080` (Railway private network) |
| `PORT` | Railway injects this; nginx template consumes it |

The frontend nginx proxies `/api`, `/oauth2`, `/login`, `/actuator` to `BACKEND_URL`, so the browser
only ever talks to the frontend domain and the session cookie stays first-party.

## 4. Google OAuth redirect URI

Add `https://<frontend-domain>/login/oauth2/code/google` (and/or the backend domain, matching
`APP_API_BASE_URL`) to the OAuth client's authorized redirect URIs.

## Secrets

No secret values are committed. `.env` is git-ignored; `.env.example` documents the keys.
