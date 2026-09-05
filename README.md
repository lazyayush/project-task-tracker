# Waypoint

A multi-project task tracking application for teams juggling several client engagements at once — managers set up projects and see the whole portfolio at a glance; staff see what's theirs and move it forward. Every architectural decision, trade-off, and known limitation is documented in `docs/`.

## What it does

- **Accounts & roles** — email/password auth, JWT-based, with server-enforced Manager/Member permissions
- **Projects** — create, edit, archive/restore (archiving hides without deleting), manager-controlled membership
- **Tasks** — full CRUD within a project, with a same-project "blocking" relationship between tasks
- **Lifecycle** — Backlog → In Progress → In Review → Done, with Blocked/unblock and reopen support; the server is the single source of truth for which moves are legal, and the UI only ever offers what the server currently allows
- **Assignment** — many-to-many task assignment, scoped to project members, with a cross-project "My Tasks" view
- **Search** — full-text search plus filters (project, status, assignee, priority, overdue), sort, and server-side pagination across every project a user can see
- **Bulk actions** — status/assignee/due-date changes applied to many selected tasks at once, with per-task success/failure reporting; plus CSV export of the current filtered view
- **Dashboard** — headline counts, status/assignee breakdowns, and an 8-week completions trend
- **Immutable history** — every task's full timeline (field changes, assignments, comments) is permanently recorded; a database trigger — not just application code — makes it genuinely un-editable, including by managers
- **Overdue alerts** — a portfolio-wide (or project-scoped, for members) alerts view with per-user dismissal that resets automatically if a task's due date changes

## Tech stack

| | |
|---|---|
| Backend | Java 21, Spring Boot 4, Gradle, Spring Security (JWT), Spring Data JPA |
| Database | PostgreSQL, schema managed by Flyway migrations |
| Frontend | React, TypeScript, Vite, Tailwind CSS v4, React Router, Recharts |
| Dev tooling | Swagger/OpenAPI UI, ESLint |

No ORM auto-schema generation — every table exists because a Flyway migration created it, deliberately, with reviewable history.

## Project structure

```
.
├── task-tracker-backend/     Spring Boot API
├── task-tracker-frontend/    React app
├── docs/
│   ├── architecture.md       Moving pieces, request paths, diagram, what we chose not to build
│   ├── schema.md             Every table, relationships, constraints, ERD
│   ├── decisions.md          Real decisions made, alternatives rejected, and why
│   ├── plan.md                Session-by-session build log
│   └── ai-prompts.md         Every significant AI prompt used, including ones that produced bad output
└── SUBMISSION.md             Live URLs, demo credentials, deployment notes
```

## Running locally

**Prerequisites:** Java 21, Node 18+, Docker.

**1. Database**
```bash
cd task-tracker-backend
docker compose up -d
```

**2. Backend**
```bash
./gradlew bootRun
```
Runs on `http://localhost:8080`. Flyway applies all migrations automatically on startup. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html` for exploring the API directly.

**3. Frontend**
```bash
cd task-tracker-frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`. Set `VITE_API_BASE_URL` in a `.env` file if the backend isn't on the default port.

## Architecture, at a glance

Single-module Spring Boot backend, stateless JWT auth, two layers of authorization (coarse role-based URL rules, plus data-scoped service-layer checks for anything a URL pattern can't express — like "is this user actually a member of this specific project"). 

## Documentation

This project treats its `docs/` folder as seriously as its code — `docs/decisions.md` in particular records real trade-offs made along the way (including two decisions that were built one way and later reversed after testing or re-reading the brief surfaced a better answer), and `docs/ai-prompts.md` logs AI usage transparently, including prompts that produced incorrect or incomplete results and what was done about it.

## Demo

See `SUBMISSION.md` for the live URL and demo credentials for both roles.