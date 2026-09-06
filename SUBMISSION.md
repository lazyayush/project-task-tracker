# Submission

## Links

- **GitHub repository:** `https://github.com/lazyayush/project-task-tracker`
- **Live application:** `https://project-task-tracker-three.vercel.app`

## Notes for the reviewer

- The backend runs on Render's free tier and sleeps after 15 minutes of inactivity. A GitHub Actions workflow pings /health every 10 minutes to keep it warm, but after a deploy or extended inactivity, the first request may still take 30–60 seconds. 
  Please wait a moment on the first load rather than assuming the application is broken.
- The database is hosted on Supabase's free tier rather than Render's, since Render's free Postgres
  expires (and its data is deleted) after 30 days — chosen specifically so this stays reachable for
  review beyond a short window.
- The dashboard currently aggregates by loading all visible tasks into the JVM and computing counts
  in-memory, rather than SQL `GROUP BY` queries — fine at demo scale, flagged in `docs/schema.md` as the
  first thing that would need to change at real data volume, not swept under "just hosting."
- Role permissions are layered, not flat: project creation and task creation/metadata edits are
  manager-only, but managing a task's *assignments* is scoped differently — either the project's owner
  (regardless of role) or a manager who is also a member of that specific project. This is deliberate
  (see `docs/decisions.md`), not an inconsistency — owner and role grant different, overlapping kinds of
  authority on purpose.
- All 10 required goals are implemented and tested (see checklist below).

## Demo credentials

| Role    | Email               | Password |
|---------|---------------------|---|
| Manager | `manager1@test.com` | `manager123` |
| Manager | `manager2@test.com`    | `manager456` |
| Member  | `james@test.com`    | `james123` |
| Member  | `thomas@test.com`    | `thomas123` |
| Member  | `alex@test.com`    | `alex1234` |
| Member  | `eddy@test.com`    | `eddy1234` |
| Member  | `bob@test.com`    | `bob12345` |

## Stack

| Layer | What you used | Why |
| --- | --- | --- |
| Frontend | React (Vite, TypeScript) + Tailwind CSS v4 + React Router + Recharts | Fast to build with; Recharts added only once a real charting need (the dashboard trend) justified the dependency |
| Backend | Java 21 + Spring Boot 4 + Spring Security (JWT) + Spring Data JPA | Familiar stack; single Gradle module (see `docs/decisions.md`) rather than microservices, since scope didn't justify the overhead |
| Database | PostgreSQL, schema owned by Flyway migrations | Rejected Hibernate `ddl-auto` in favor of reviewable, versioned SQL migrations — see `docs/decisions.md` #1 |
| Hosting | Vercel (frontend), Render (backend, via Docker — no native Java buildpack), Supabase (database) | All free-tier; Supabase specifically chosen over Render Postgres for its permanent free tier |

## Goal checklist

| # | Goal | Status | Notes |
| --- | --- | --- | --- |
| 1 | Accounts and roles | Done | JWT auth; role enforced via Spring Security URL rules AND service-layer checks for anything URL patterns can't express (e.g. project membership) |
| 2 | Projects | Done | CRUD + archive/restore; archived projects retain all data and tasks |
| 3 | Tasks inside projects | Done | CRUD + same-project blocking relationship, DB-enforced against self-referencing |
| 4 | Task lifecycle with rules | Done | Explicit transition-rules map + per-task unfinished-blocker check for Done; legal moves queried by the frontend, never hardcoded client-side |
| 5 | Assignment | Done | Many-to-many, project-membership-restricted; removing a member cascades to unassign their tasks in that project |
| 6 | Finding things | Done | Server-side Specification-based search: text, 5 filters, 3 sort options, real pagination with total count |
| 7 | Bulk actions + CSV export | Done | Per-task success/failure reporting; bulk actions run through a dedicated executor bean with `REQUIRES_NEW` per task to guarantee genuine partial-success isolation (see `docs/decisions.md`) |
| 8 | Dashboard | Done | Headline counts, status/assignee breakdowns, 8-week completions trend |
| 9 | Immutable history | Done | Append-only, no setters on the entity, no update/delete repository methods, AND a Postgres trigger that rejects UPDATE/DELETE at the database level — verified by attempting tampering directly via psql, not just trusted |
| 10 | Overdue alerts | Done | Portfolio-scoped visibility (reversed from an initial personal-only design — see `docs/decisions.md`), dismissal restricted to actual assignees, resets globally when a due date changes |

## How much time did you actually spend?

Around 14-16 hours.


## Seed data
2 managers, 5 members, 3 projects (1 archived), each having 9 tasks assigned to different project members.

## What would you do next, with another 12 hours?

- Add project name/key to the `Task` API response so the cross-project My Tasks view can label which
  project each task belongs to — currently a real, acknowledged UX gap, not a silent omission.
- Cycle detection across chains of task dependencies (one of the brief's own stretch goals). The current
  schema only guards against a task blocking itself directly at the database level; a longer cycle
  (A blocks B, B blocks C, C blocks A) would currently go undetected and could deadlock the lifecycle
  logic in a way that's hard to diagnose from the UI alone.
- A drag-and-drop board view over the existing status columns — the lifecycle rules and legal-transition
  endpoint already exist and would need no backend changes; this is purely a frontend interaction layer
  on top of what's already there.
- General consolidation pass: a few of the permission checks scattered across `TaskService`
  (`assertManagerIsProjectMember`, `assertCanView`, `assertCanManageAssignment`, `assertProjectMember`)
  overlap enough that they're worth unifying into a single, explicit permission-evaluation component
  rather than four separate private methods with subtly different logic each caller has to pick correctly.

## What are you least happy with in this codebase, and why?

The authorization logic in `TaskService` grew organically, one permission rule at a time, as each new
feature needed a slightly different check — manager-only, manager-and-member, owner-or-manager-member,
any-member. Each rule is individually correct and was deliberately reasoned through when it was added,
but the result is four separate private methods with overlapping but non-identical logic, and it's not
obvious from a method's name alone which of the four a new caller should reach for. A cleaner design
would centralize these into one small policy component that takes an action and a context and returns
a yes/no, rather than scattering the equivalent logic across the service class as it currently stands.