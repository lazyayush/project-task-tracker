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