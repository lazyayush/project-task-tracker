# AI Prompts

## 1. Establishing Project Governance and Master Workflow

### Prompt

You are a senior software architect, lead developer, and technical mentor. We are going to build the application described in the README context previously provided, step-by-step, using a disciplined, production-grade approach.

Your goal is NOT to generate the entire application or entire phases at once.

Your role is to guide development incrementally, explain key architectural decisions, ask for preferences before building, present full production-ready code file-by-file, and act as a collaborative pair programmer who will correct me if I suggest a sub-optimal approach.

#### TECH STACK CONSTRAINTS:

Backend: Java 21, Spring Boot, Gradle

Frontend: React JS

Architecture: Clean, modular, production-ready design

Rule: Do NOT introduce new major frameworks, external databases, or third-party libraries without proposing them and asking for my approval first.

#### INCREMENTAL CODE DELIVERY RULES:

**File-by-File Code Delivery:** When we agree to implement a specific component or feature chunk, provide complete, fully written files.

**No Placeholders or Stubs:** Do NOT leave comments like `// implement logic here`, `// TODO`, or `// rest of the code remains the same`. Write the entire, runnable code for every file presented.

**One Chunk at a Time:** Deliver code in small, testable chunks (e.g., entity & repository setup first, then service layer, then controller, then UI component). NEVER dump 10+ files in a single response.

**Clean Code Standards:** Ensure proper package structures, exception handling, data transfer objects (DTOs), and separation of concerns.

#### WORKFLOW FOR EVERY PHASE & CHUNK:

For each phase of the project, we will follow this exact flow:

**Phase Architectural Overview:** Present a high-level architectural diagram/flow of what will be built in the current phase and break the phase down into sub-chunks.

**Preference & Requirement Gathering:** Ask targeted questions about my preferences for this phase (e.g., directory structures, state management, security configurations, database schema choices).

**Step Proposal:** Propose the exact first/next chunk to implement and explain why we are building it now.

**Wait for Approval:** STOP and wait for my explicit confirmation before generating any code.

**Implementation:** Provide the complete, production-grade files for that specific chunk.

**Review & Next Step:** Wait for me to confirm the chunk works or ask questions before moving to the next chunk.

#### COLLABORATION STYLE & MENTORSHIP:

**Be Corrective & Candid:** If I suggest an anti-pattern, an unscalable structure, or a bad technical decision, explicitly tell me why it's wrong and propose the industry standard solution.

**Explain Trade-offs:** Briefly explain the technical reasoning, design patterns, or architectural trade-offs behind key decisions.

**Prioritize Production Quality:** Focus on maintainability, modularity, security, and developer experience—no shortcuts or toy code.

#### YOUR FIRST RESPONSE:

Based on the README context already provided:

Briefly outline a multi-phase implementation roadmap to complete all project requirements.

Provide the high-level architectural flow for Phase 1.

Ask me 3–5 clarifying questions about my architecture, tooling, or design preferences for Phase 1.

Do NOT generate any project code yet. Wait for my answers and approval.

---

### Output

The AI accepted all execution constraints, provided a high-level multi-phase implementation roadmap, presented the architectural overview for Phase 1, and asked targeted clarifying questions regarding database migration tools, authentication strategy, and initial directory structure preferences without outputting any premature code.

---

### Correction

Nothing in the output itself—the response adhered strictly to the requested stop-and-wait behavior.


## 2. Debugging: 401 vs. 403 status codes

### Prompt

Reported that all requests to a role-protected endpoint returned 403 Forbidden, including requests with no Authorization header at all (expected 401), and also reported that even a valid token on an authenticated-only endpoint was returning 403.

---

### Output

An explanation that Spring Security's default Http403ForbiddenEntryPoint returns 403 for every access denial unless a real authentication entry point is configured, plus a custom exceptionHandling() block distinguishing authenticationEntryPoint (401) from accessDeniedHandler (403).

---

### Correction
Applied the fix as given; the 401/403 distinction started working correctly afterward. The suspected separate bug (valid tokens still returning 403 on /api/me) turned out to resolve itself once the entry-point/access-denied handlers were correctly wired — the underlying token validation logic was not actually broken.


## 3. ProjectService and ProjectController generation

### Prompt

Asked for ProjectService and ProjectController given the confirmed answers: global manager privilege (not owner-scoped), enforced project-key format (2-10 uppercase letters/digits), and a fixed owner (no reassignment after creation).

---

### Output

Delivered full project lifecycle and membership APIs (create, update, archive, restore, add-member, remove-member, list). Includes a dedicated owner-removal guard that blocks removing a project's owner from its membership list—enforced as a direct operational rule stemming from the fixed-owner design constraint.

---

### Correction
Manual testing via Swagger UI caught an authorization flaw where MEMBER users could pass includeArchived=true on GET /api/projects to view archived projects. The issue stemmed from the controller blindly accepting the query parameter without checking caller permissions. Fixed at the controller/service boundary by deriving the flag server-side (effectiveIncludeArchived = isManager && requestedIncludeArchived), enforcing role restrictions regardless of raw request inputs.


## 4. Bulk Assignee Change Semantics

### Prompt

Asked whether a bulk “assignee change” should REPLACE all existing assignees on each task or ADD the specified user alongside the existing assignees. The README did not explicitly define the behavior, so the decision was based on the wording and consistency with the other bulk actions.

---

### Output

Implemented assignee changes with REPLACE semantics. The specified user becomes the task's assignee and existing assignments are removed first.


## 5. Overdue Alerts Design

### Prompt

Asked for an architectural overview and clarification on whether overdue alerts should be personal or portfolio-wide.

---

### Output

Initially recommended personal-only alerts because they were simpler and avoided ambiguity around manager visibility.

---

### Correction
Re-read the brief and realized managers need portfolio-wide visibility to answer questions like “what is overdue?” and “who is overloaded?”. Revised the design to portfolio/project-scoped alerts, while keeping dismissal restricted to the viewer’s own assignments.


## 6. Frontend - Authentication & Dashboard

### Prompt

Requested the frontend project setup with Vite/React/TypeScript, routing, authentication, API client, and dashboard.

---

### Output

Received the required frontend files and structure for the project setup, authentication flow, and dashboard implementation.

The frontend included the Vite/React/TypeScript scaffold, API client, `AuthContext`, protected routes, login/register pages, and dashboard connected to the backend API.


## 7. API Client Empty-Body Error

### Prompt

Reported a misleading "could not add member" error where the member was actually added successfully. Requested investigation and a general fix for the issue.

---

### Output

Diagnosed the issue in the shared `apiRequest` function, which assumed empty responses would always use HTTP 204. The add-member endpoint returned HTTP 201 with no response body, causing JSON parsing to fail after a successful request.
Fixed the API client to detect actual empty response content instead of relying only on the status code. This also prevents the same issue from affecting other no-body endpoints such as member removal and task unassignment.

