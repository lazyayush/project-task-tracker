# Plan

## Phase 1 — Project Scaffolding, Core Domain & Security Baseline

### How did you break the work into sessions?
* **Planning & Architecture:** Reviewed requirements, mapped system flows, and established the stack (Java 21, Spring Boot 4.0.6, Gradle, React, Docker Postgres, Flyway, JWT).
* **Scaffolding & Core Auth (Chunks 1–4):** Built base project structure, Flyway migrations, `User` entity/repository, BCrypt hashing, JWT issuance, `/api/auth/**` endpoints, and Security Filter Chains.
* **Debugging (401 vs. 403 Status Codes):** Identified an issue where unauthenticated requests incorrectly returned 403 instead of 401. Resolved this by overriding Spring Security's default `Http403ForbiddenEntryPoint` with custom `authenticationEntryPoint` (401) and `accessDeniedHandler` (403) configuration.

### What order did you build in, and why that order?
1. **Scaffolding & Migration Baseline:** Verified database connectivity and schema tracking in isolation before writing business domain code.
2. **User Entity & Data Access:** Established core persistence layers prior to introducing security filters.
3. **Auth & Security Filters:** Implemented identity and access control ahead of all other goals, as downstream features (projects, tasks, search, dashboard) rely directly on role authorization.

### What did you estimate versus what it actually took?
* **Estimated:** Quick setup for baseline scaffolding and security.
* **Actual:** Took longer than expected strictly due to debugging Spring Security's default status code handling (401 vs. 403 entry point behavior) when wiring up role-protected routes.

### What did you cut when you ran short?
* **Status:** Nothing cut. Phase 1 (Accounts, roles, and security baseline) is 100% complete and verified.

---

## Phase 2 — Projects Domain & Membership Infrastructure

### How did you break the work into sessions?
* **Entities & Migrations:** Modeled `Project` and explicit `ProjectMember` entities (capturing `joined_at` for future audit logging) along with Flyway schema scripts.
* **Service & Authorization Rules:** Defined core domain rules—global manager permissions, fixed project keys, and immutable ownership—before implementing `ProjectService` and `ProjectController` CRUD/membership endpoints.
* **Tooling & Bug Resolution:** Integrated Swagger/OpenAPI for testing. Diagnosed and fixed a privacy leak where `MEMBER` users could pass `includeArchived=true` to bypass manager-only visibility constraints.

### What order did you build in, and why that order?
1. **Schema & Persistence (`Project` / `ProjectMember`):** Built entities and Flyway migrations first to establish referential constraints and explicit join relationships.
2. **Business Logic & Layered Security:** Built `ProjectService` next to enforce fine-grained data checks (membership, archiving) on top of coarse `SecurityConfig` route rules.
3. **API & OpenAPI Tooling:** Added endpoints and Swagger UI last to enable rapid, end-to-end testing against real HTTP payloads.

### What did you estimate versus what it actually took?
* **Estimated:** Straightforward CRUD controller and service implementation.
* **Actual:** Took longer than planned due to manual testing edge cases in Swagger, which revealed an authorization bypass flaw (`includeArchived` visibility enforcement) that required refining service-layer authorization logic.

---

## Phase 3 — Task CRUD and blocking.

### How did you break the work into sessions?
Phase 3 was executed as a dedicated session focused on establishing the core task domain and its lifecycle state machine. The session was divided into upfront architectural resolution of permission rules (metadata vs. status split), database and entity creation, transition logic implementation, and end-to-end testing of the full transition and dependency flow.

### What order did you build in, and why that order?
1. **Permission & Boundary Design:** Resolved the metadata-edit vs. status-transition split upfront so authorization rules were clear before writing any code.
2. **Domain & Data Layer:** Built the Task and TaskBlock entities and generated database migrations to lay down the underlying data model.
3. **Service & Controller Layer:** Built the CRUD APIs, inter-task dependency validation (TaskBlock), and state transition rules.
4. **Lifecycle Testing:** Rigorously tested the complete status transition sequence (legal next-states, reopen pathways, blocking checks).

### What did you estimate versus what it actually took?
* **Estimated:** Building the basic Task/TaskBlock CRUD endpoints went as estimated. However, rigorous testing of edge-case status transitions (reopening tasks, preventing backward moves from IN_REVIEW, and dependency checks) added unexpected time.
* **Actual:** Took longer than planned due to manual testing edge cases in Swagger, which revealed an authorization bypass flaw (`includeArchived` visibility enforcement) that required refining service-layer authorization logic.

---

## Phase 4 — Search, Filter, Sort, and Pagination

### How did you break the work into sessions?
Focused on designing dynamic task search, implementing reusable JPA Specifications, adding sorting/pagination, and testing authorization and filter behavior.

### What order did you build in, and why that order?
1. **Authorization Design:** Defined project visibility rules for members and managers.
2. **Specification Layer:** Implemented reusable filters for project, status, priority, text search, assignee, and overdue tasks.
3. **Sorting & Pagination:** Added dynamic sorting and pagination with a page-size cap of 100. Priority sorting uses CASE WHEN to ensure severity order instead of alphabetical order.
4. **Testing:** Tested filter combinations, text search, overdue behavior, priority sorting, pagination limits, and manager/member visibility.

### What did you estimate versus what it actually took?
* **Estimated:** Basic filtering, sorting, and pagination were expected to be straightforward.
* **Actual:** Implementation went largely as planned, with additional attention required for priority ranking and validating authorization boundaries and combined filters.

---

## Phase 5 — Bulk Actions and CSV Export

### How did you break the work into sessions?
Focused on defining bulk-action semantics, implementing per-task transaction isolation, adding CSV export, and testing partial success and authorization behavior.

### What order did you build in, and why that order?
1. **Action Semantics:** Defined status, assignee, and due-date changes as replacement-style operations, with assignee changes replacing the existing assignee.
2. **Transaction Isolation:** Added a separate TaskBulkActionExecutor with REQUIRES_NEW so each task runs in its own transaction. This prevents one failed task from rolling back successful tasks and avoids Spring's self-invocation limitation.
3. **Bulk Execution:** Kept applyBulkAction() non-transactional so it coordinates independent task operations and records each success or failure.
4. **CSV Export:** Reused buildSpecification() from search to keep filtering and visibility rules consistent, while exporting all matching tasks without pagination.
5. **Testing:** Prioritized partial-success tests and re-fetched successful tasks to verify that changes actually persisted. Also tested invalid assignees and CSV filtering/escaping.

### What did you estimate versus what it actually took?
* **Estimated:** Bulk actions and CSV export would be straightforward by reusing existing task operations and search filters.
* **Actual:** Took longer due to designing and verifying REQUIRES_NEW transaction isolation, particularly testing persistence rather than relying only on the response.

---

## Phase 6 — Dashboard

### How did you break the work into sessions?
Focused on fixing completion tracking, defining dashboard metrics and visibility rules, implementing the aggregations, and reviewing scalability.

### What order did you build in, and why that order?
1. **Completion Tracking:** Added completed_at because updated_at cannot reliably identify when a task was completed. Integrated it into the existing status-transition logic.
2. **Metric Definitions:** Defined weeks as Monday–Sunday in UTC and clarified assignee counting, including an Unassigned bucket and counting multi-assignee tasks under each assignee.
3. **Dashboard Implementation:** Built the headline metrics, status and assignee breakdowns, and 8-week completion trend using the existing project-visibility rules.

### What did you estimate versus what it actually took?
* **Estimated:** Dashboard aggregation would be straightforward after defining the metrics.
* **Actual:** Took longer due to the required completed_at schema change and testing.

---

## Phase 7 — Task History & Comments

### How did you break the work into sessions?
Focused on building the history foundation and immutability guarantee, creating the history service, integrating logging across existing task mutations, adding comments, and exposing the task timeline.

### What order did you build in, and why that order?
1. **History Foundation:** Added the TaskHistoryEntry entity and migration, including triggers to enforce immutability at the database level.
2. **History Service and Task Integration:** Built TaskHistoryService with separate methods for each event type so existing task operations could log changes consistently.
3. **Comments:** Added task comments as COMMENT history events rather than introducing a separate comments table.
4. **Timeline:** Added the history endpoint to return the complete chronological task timeline and verified the recorded events.

### What did you estimate versus what it actually took?
The phase was completed in the estimated time.

---

## Phase 8 — Overdue Alerts

### How did you break the work into sessions?
Focused on resolving alert-scope ambiguity, building dismissals and reset behavior, reviewing unused code, and checking for duplicated business logic.

### What order did you build in, and why that order?
1. **Alert Scope:** Initially chose personal-only alerts, then reversed to project/portfolio-scoped visibility after re-reading the brief.
2. **Dismissals:** Added AlertDismissal as mutable current state and implemented automatic dismissal resets when a task's due date changes.

### What did you estimate versus what it actually took?
The phase required more iteration than initially expected because the alert-scope decision was reversed mid-phase. 

---

## Phase 9 — Frontend(Authentication and Dashboard)

### How did you break the work into sessions?
I split the work into setup, authentication, UI/design, dashboard development, and final bug-fixing/refinement sessions.

### What order did you build in, and why that order?
I followed the dependency order: project setup → API client → authentication → routing → design system → dashboard → refinement. This ensured each layer was ready before building features that depended on it.

### What did you estimate versus what it actually took?
I expected most of the time to go toward feature development. In practice, debugging Tailwind/Vite issues, fixing UI inconsistencies, and refining the design took more time than expected.

---

## Phase 10 — Projects List Page

### How did you break the work into sessions?
I split the work into the project list UI, manager-only controls, reusable components, and final UI refinement.

### What order did you build in, and why that order?
I built the project list, manager-only create/archive controls, project cards, and the reusable `Badge` component. This followed the existing frontend structure and prepared the UI for the upcoming project detail and task features.

### What did you estimate versus what it actually took?
The phase was completed in the estimated time.

