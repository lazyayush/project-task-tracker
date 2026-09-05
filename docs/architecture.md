# Architecture

## Moving pieces and how they talk to each other

**Frontend — React (Vite, TypeScript, Tailwind CSS v4).** Six pages (Dashboard, Projects, Project Detail, My Tasks, Alerts, Search) plus a self-sufficient `TaskDetailModal` reused across three different pages without any of them needing to pre-load its context. All server communication funnels through a single `apiRequest()` client function, which attaches the JWT bearer token, redirects to `/login` on any 401, and parses responses based on actual content rather than assuming status-code/body pairing. `AuthContext` validates any stored token against `GET /api/me` on load rather than trusting a token's mere presence in `localStorage`.

**Backend — Spring Boot (single Gradle module).** One deployable process; internal package boundaries (`web` / `service` / `repositories` / `security` / `entity`) enforce separation by convention. `SecurityConfig` combines JWT authentication, coarse role-based URL-pattern rules, CORS. Every service layers two kinds of authorization: URL-pattern rules answer "can this role hit this endpoint," service-layer checks answer data-scoped questions URL patterns can't express ("does this user belong to this project," "is this the project's owner").

**Database — Postgres**, schema owned entirely by Flyway migrations (`V1`–`V10`, applied automatically on startup, validated against by Hibernate in `validate`-only mode — Hibernate never mutates schema). One table, `task_history`, carries a database-level `BEFORE UPDATE/DELETE` trigger that unconditionally raises an exception — the audit trail's immutability is a structural database guarantee, not application-code discipline.

**Swagger/OpenAPI UI** — mounted for manual, dev-time endpoint testing with a reusable bearer-token security scheme; explicitly not part of the production request path.

## Where each piece runs


## Request paths — representative user actions, end to end

**1. Login and every authenticated request thereafter.**
```
POST /api/auth/login {email, password}
  → AuthController → AuthService: BCrypt.matches → JwtService issues a signed JWT
  → Frontend stores it, AuthContext holds { email, role } in memory

Every subsequent request:
  apiRequest() attaches "Authorization: Bearer <token>"
  → JwtAuthenticationFilter validates + loads the user, populates SecurityContext
  → SecurityConfig's authorizeHttpRequests rules gate by role where applicable
  → Controller → Service → Repository → Postgres
```

**2. A manager creates a project.**
```
POST /api/projects {key, name, ownerEmail}
  → role-gated MANAGER-only at SecurityConfig
  → ProjectController → ProjectService.create(), one @Transactional boundary:
      - uniqueness check (also DB-enforced) → save Project
      - auto-create a ProjectMember row for the owner
  → Frontend's ProjectsPage re-fetches the list
```

**3. Any project member moves a task through its lifecycle — the state machine, queried, not duplicated client-side.**
```
Frontend: TaskStatusControl fetches GET /api/tasks/{id}/legal-transitions
  → renders ONLY those options — the literal implementation of the README's
    "the interface should only offer the moves that are currently legal"

On selection: PATCH /api/tasks/{id}/status {status}
  → TaskService.transitionStatus(): checked against TaskTransitionRules'
    static map, PLUS the per-task unfinished-blocker check for Done
  → on success: completedAt set/cleared, a FIELD_CHANGED history entry
    written, any alert_dismissals for this task's due date left untouched
    (only a due-date CHANGE resets those, not a status move)
```

**4. A bulk action**
```
POST /api/tasks/bulk-action {taskIds: [...], actionType, ...}
  → TaskService.applyBulkAction() loops the IDs, calling
    TaskBulkActionExecutor.applyOne() — a SEPARATE Spring bean, called through
    its own proxy, each invocation wrapped in @Transactional(REQUIRES_NEW)

  Why a separate bean, not a private method: Java self-invocation (calling
  this.someMethod() from within the same class) bypasses Spring's transactional
  proxy entirely. Without this separation, one failing task in a batch could
  silently affect the transactional boundaries of others, breaking the
  README's explicit "report per task what succeeded and what was rejected"
  requirement — which depends on each task's change genuinely committing (or
  rolling back) independently of its neighbors in the same batch.

  → Frontend renders only the FAILED entries with reasons, resolving task
    IDs back to titles from the already-loaded search results
```

**5. Immutable history — enforced below the application layer, not just within it.**
```
Any mutation (status change, field edit, assign/unassign, comment)
  → TaskHistoryService.record*() — INSERT only; the entity has no @Setter,
    the repository has no update/delete methods, and even if application
    code somehow attempted UPDATE/DELETE against task_history directly,
    a Postgres trigger raises an exception and rolls back the attempt.
  → Three independent layers enforcing the same guarantee, verified in
    testing by attempting the tampering directly via psql, not just trusting
    that the application "shouldn't" do it.
```

## What I decided not to build, and why

- **Manager-approval or invite-only registration** — self-registration allows choosing MANAGER directly; a deliberate simplification since every account here is one the developer controls, explicitly not a pattern appropriate outside this context.
- **Server-side filtering/pagination for the project list** (only the task list has it) — goal 6 requires it for tasks specifically, not projects; the project list's in-memory filtering is a named, acceptable shortcut at this scale.
- **A single shared "overdue" definition** — acknowledged as duplicated across the dashboard, task search, and alerts (three independent implementations of `dueDate < now AND status != DONE`), rather than extracted into one shared helper; a known, minor maintainability gap rather than an oversight.
- **httpOnly cookies for the JWT** — chosen localStorage instead for simplicity, with the trade-off (readable by page JS in an XSS scenario) explicitly acknowledged rather than treated as risk-free.
