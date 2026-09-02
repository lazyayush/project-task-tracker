# Decisions

## Decision 1

- **Chose:** Local PostgreSQL via Docker Compose paired with Flyway migrations (ddl-auto: validate).
- **Rejected:** In-memory H2 database with Hibernate ddl-auto: update.
- **Why:** In-memory H2 with ddl-auto: update was ideal for rapid initial scaffolding, but switching to Dockerized Postgres ensures environment parity with production. Flyway SQL scripts (V1__...sql) provide explicit, version-controlled schema tracking, unlike implicit runtime inference.
- **Later reversed:** Initially planned to use in-memory H2 to avoid local setup overhead, but switched the strategy to Dockerized Postgres and Flyway before implementation to guarantee production parity and strict schema control from day one.

## Decision 2

- **Chose:** Database-level UNIQUE constraint on email (uq_users_email).
- **Rejected:** Application-level validation only (e.g., checking existsByEmail in the service layer before save).
- **Why:** Application-level checks are susceptible to race conditions under concurrent requests.

## Decision 3

- **Chose:** Database-level CHECK constraint for user roles (chk_users_role restricting values to 'MANAGER' and 'MEMBER').
- **Rejected:** Plain unconstrained VARCHAR or application-only Enum mapping without DDL checks.
- **Why:** Database CHECK constraints prevent invalid or unsupported role strings from entering the system via manual SQL queries, scripts, or external integrations.

## Decision 4

- **Chose:** Allow a user to pick their own role (MANAGER or MEMBER) at registration.
- **Rejected:** Restricting manager-role accounts to seed data or an invite/promotion flow.
- **Why:** In a real product, letting any user self-assign MANAGER (with its task-deletion and project-membership powers) would be a genuine privilege-escalation vulnerability. Accepted here as a deliberate simplification since every account in this system is one under the developer's own control for demo purposes.

## Decision 5

- **Chose:** Global manager privileges (any manager can edit, archive, or manage membership across any project).
- **Rejected:** Restricting manager administrative actions strictly to projects they personally own.
- **Why:** Matches the literal spec requirement ("Managers can create and archive projects...") and eliminates redundant ownership-check logic across service layers.

## Decision 6

- **Chose:** Splitting task permissions into separate rules for metadata edits (manager-only) and status transitions ( open to all project members).
- **Rejected:** Treating "edit" as a single blanket manager-only rule.
- **Why:** Blanket edits prevent staff from moving tasks forward and break per-user task tracking.

## Decision 7

- **Chose:** Requiring a manager to be explicitly assigned as a project member in order to create, edit, or delete tasks within that project.
- **Rejected:** Extending global manager administrative privileges directly to task-level actions.
- **Why:** Non-member managers can administer the project, but shouldn't alter its individual tasks.

## Decision 8

- **Chose:** Returning a reopened DONE task directly to IN_PROGRESS.
- **Rejected:** Returning a reopened DONE task to BACKLOG.
- **Why:** Reopening a completed task implies resuming active work rather than sending it back to unprioritized planning.

## Decision 9

- **Chose:** Task assignments can be managed by either the project owner or any manager who is an explicit member of that project.
- **Rejected:** Strictly manager-only authority or fully open authority to all project members.
- **Why:** Assignment directly impacts accountability and user dashboards, making it more sensitive than status updates but less restricted than core metadata edits. It also exercises the owner role separation: allowing non-manager owners to assign work within their own projects without giving open assignment rights to general project members.

## Decision 10

- **Chose:** Assignee changes use REPLACE semantics, removing the existing assignee and assigning the specified user.
- **Rejected:** ADD semantics, where the new user would be added alongside existing assignees.
- **Why:** The bulk actions are all replace-style mutations: a status change sets a new status, a due-date change sets a new due date, and an assignee change naturally means reassigning the task. This also matches the conventional meaning of “bulk reassign” in task-management tools.

## Decision 11

- **Chose:** byStatus includes all tasks, including Done, while byAssignee excludes Done tasks.
- **Rejected:** Using the same scope for both breakdowns.
- **Why:** The status breakdown is intended to show the complete task distribution, whereas the assignee breakdown represents current workload. Including completed tasks in the latter would turn it into a historical completion leaderboard rather than a view of active responsibility.

## Decision 12

- **Chose:** Database-trigger-enforced immutability for task_history using BEFORE UPDATE and BEFORE DELETE triggers.
- **Rejected:** Application-only protection that relies on developers avoiding updates and deletes.
- **Why:** History is an audit trail and must never be altered after creation. Application rules can be bypassed by future code or database operations, whereas triggers enforce immutability at the database level.
