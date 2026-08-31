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