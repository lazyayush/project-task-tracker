# Schema

## Table: `users`

### Columns & Data Types
* **`id`** (`BIGSERIAL`): 64-bit auto-incrementing primary key.
* **`email`** (`VARCHAR(255)`): Unique user email address.
* **`password_hash`** (`VARCHAR(255)`): Hashed credentials.
* **`role`** (`VARCHAR(20)`): User authorization level (`MANAGER` or `MEMBER`).
* **`created_at`** (`TIMESTAMPTZ`): Record creation timestamp in UTC.

### Relationships
- **One-to-many:** `User → Project` via `owner_id` (a user can own many projects; a project has exactly one owner).
- **Many-to-many:** `Project ↔ User` via `project_members` (a project has many members; a user belongs to many projects).

### Constraints & Architecture Boundaries
* **Database-enforced:** `PRIMARY KEY`, `NOT NULL`, `uq_users_email` (uniqueness), `chk_users_role` (allowed role values). Regradless of input source it guarantees absolute data integrity.
* **Application-enforced:** Password hashing algorithms, email string patterns, and workflow authorization.

### Deliberate Denormalization
* **Inline `role` column:** Stored directly as a string instead of using a separate `roles` lookup table with foreign keys to avoid redundant `JOIN` operations during authentication.

### Bottlenecks at 100x Scale
* **Unindexed reads:** Searching by `role` or sorting by `created_at` will trigger expensive full table scans without secondary indexes.
* **Storage & RAM footprint:** Disk space and buffer pool RAM usage will spike due to large `VARCHAR(255)` columns and growing primary key / unique indexes.

---

## Table: `projects`

### Columns & Data Types
* **`id`** (`BIGSERIAL`): Primary key.
* **`project_key`** (`VARCHAR(20)`): Unique project key (format enforced in app, uniqueness in DB). Named `project_key` to avoid the SQL reserved word `KEY`.
* **`name`** (`VARCHAR(255)`): Project name (NOT NULL).
* **`description`** (`TEXT`): Project description (Nullable).
* **`owner_id`** (`BIGINT`): Foreign key referencing `users.id` (NOT NULL, `ON DELETE NO ACTION`).
* **`archived`** (`BOOLEAN`): Soft-delete flag (NOT NULL, `DEFAULT FALSE`).
* **`created_at`** (`TIMESTAMPTZ`): UTC creation timestamp (NOT NULL, `DEFAULT now()`).
* **`updated_at`** (`TIMESTAMPTZ`): UTC last-updated timestamp (NOT NULL, `DEFAULT now()`).

### Relationships
* **User → Project (`owner_id`): One-to-Many.** A user can own multiple projects, but each project has exactly one permanent owner.

### Constraints & Architecture Boundaries
* **Database-enforced:** `PRIMARY KEY`, `NOT NULL`, `uq_projects_key` (`UNIQUE`), and `owner_id` FK (`ON DELETE NO ACTION`). Protects referential integrity and prevents race conditions from concurrent creations.
* **Application-enforced:** `project_key` format validation (2–10 uppercase alphanumeric chars) and role-based visibility (`includeArchived` restricted to Managers).

### Bottlenecks at 100x Scale
* **In-Memory Filtering:** `ProjectService.listVisibleTo` loads records using `findAll()` and filters archived status via Java Streams rather than SQL. At scale, this causes high JVM memory usage and CPU overhead.

---

## Table: `project_members`

### Columns & Data Types
* **`id`** (`BIGSERIAL`): Primary key.
* **`project_id`** (`BIGINT`): Foreign key referencing `projects.id` (NOT NULL, `ON DELETE NO ACTION`).
* **`user_id`** (`BIGINT`): Foreign key referencing `users.id` (NOT NULL, `ON DELETE NO ACTION`).
* **`joined_at`** (`TIMESTAMPTZ`): UTC join timestamp (NOT NULL, `DEFAULT now()`).

### Relationships
* **Project ↔ User (`project_members`): Many-to-Many.** Explicit join entity connecting users and projects.

### Constraints & Architecture Boundaries
* **Database-enforced:** `PRIMARY KEY`, `NOT NULL`, FKs referencing `projects.id` and `users.id`, and `uq_project_members_project_user` (`UNIQUE(project_id, user_id)`). The unique constraint prevents race conditions leading to duplicate memberships.
* **Application-enforced:** Friendly error message handling before falling back on database constraint violations.

### Bottlenecks at 100x Scale
* **Unindexed `user_id` Lookups:** The `UNIQUE(project_id, user_id)` index only speeds up queries starting with `project_id`. Fetching "all projects for User X" via `findByUserId` requires full sequential table scans without an explicit index on `user_id`.

---

## Table: `tasks`

### Columns & Data Types
* **`id`** (`BIGSERIAL`): Primary key.
* **`project_id`** (`BIGINT`): Foreign key referencing `projects.id` (NOT NULL).
* **`title`** (`VARCHAR(255)`): Task name (NOT NULL).
* **`description`** (`TEXT`): Detailed details (Optional).
* **`priority`** (`VARCHAR(10)`): Enum (`LOW`, `MEDIUM`, `HIGH`) (NOT NULL).
* **`due_date`** (`TIMESTAMPTZ`): Task deadline (Optional).
* **`status`** (`VARCHAR(20)`): Enum (`BACKLOG`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`, `BLOCKED`) (NOT NULL).
* **`blocked_from_status`** (`VARCHAR(20)`): Pre-blocked state (`IN_PROGRESS`, `IN_REVIEW`) (Optional).
* **`created_at`** (`TIMESTAMPTZ`): UTC creation timestamp (NOT NULL, `DEFAULT now()`).
* **`updated_at`** (`TIMESTAMPTZ`): UTC update timestamp (NOT NULL, `DEFAULT now()`).

### Relationships
* **Project ↔ Task (`tasks`): One-to-Many.** Each project contains multiple tasks; each task belongs strictly to one project.
* **Task ↔ Task (`TaskBlock`): Many-to-Many.** Inter-task dependencies are externalized to a separate join table (not defined in this schema).

### Constraints & Architecture Boundaries
* **Database-enforced:** `PRIMARY KEY`, `NOT NULL`, FK referencing `projects.id`, and `CHECK` constraints (`chk_tasks_priority`, `chk_tasks_status`, `chk_tasks_blocked_from_status`). Prevents data corruption and invalid enum values.
* **Application-enforced:** Permission guards (manager-only metadata edits vs. member status transitions), task-blocking dependency rules, and context-aware state updates for `blocked_from_status`. Keeps complex business logic out of SQL.

### Deliberate Denormalisation
* Caching `blocked_from_status` directly on the task row avoids audit-log lookups during unblocking. Using string ENUMs with `CHECK` constraints avoids external `JOIN` tables for static statuses.

### Bottlenecks at 100x Scale
* **Unindexed Composite Queries:** The single-column `idx_tasks_project_id` index fails on combined filter/sort queries (e.g., `WHERE project_id = X AND status = Y ORDER BY due_date`), causing expensive file-sorts.

---

## Table: `task_blocks`

### Columns & Data Types
* **`id`** (`BIGSERIAL`): Primary key.
* **`blocking_task_id`** (`BIGINT`): Foreign key referencing `tasks.id` (NOT NULL) representing the prerequisite task.
* **`blocked_task_id`** (`BIGINT`): Foreign key referencing `tasks.id` (NOT NULL) representing the dependent task.
* **`created_at`** (`TIMESTAMPTZ`): UTC creation timestamp (NOT NULL, `DEFAULT now()`).

### Relationships
* **Task ↔ Task (`task_blocks`): Many-to-Many Self-Reference.** Joins tasks to other tasks to establish dependency relationships (a task can block many tasks and be blocked by many tasks).

### Constraints & Architecture Boundaries
* **Database-enforced:** `PRIMARY KEY`, `NOT NULL`, FKs referencing `tasks.id`, `uq_task_blocks_pair` (`UNIQUE(blocking_task_id, blocked_task_id)`), and `chk_task_blocks_not_self` (`CHECK(blocking_task_id != blocked_task_id)`). Prevents duplicate dependency pairs and self-blocking tasks.

---

## Table: `task_assignees`

### Columns & Data Types
* **`id`** (`BIGSERIAL`): Primary key.
* **`task_id`** (`BIGINT`): Foreign key referencing `tasks.id` (NOT NULL).
* **`user_id`** (`BIGINT`): Foreign key referencing `users.id` (NOT NULL).
* **`assigned_at`** (`TIMESTAMPTZ`): UTC assignment timestamp (NOT NULL, `DEFAULT now()`).

### Relationships
* **Task ↔ User (`task_assignees`): Many-to-Many.** A single task can be assigned to multiple users, and a single user can be assigned to multiple tasks. This join table explicitly resolves that many-to-many relationship.

### Constraints & Architecture Boundaries
* **Database-enforced:** `PRIMARY KEY`, `NOT NULL`, FKs referencing `tasks.id` and `users.id`, and `uq_task_assignees_task_user` (`UNIQUE(task_id, user_id)`). The unique constraint prevents duplicate user assignments on a single task.
* **Application-enforced:** Project membership validation (ensuring an assignee belongs to the task's parent project) and permission checks for assigning/unassigning users.


### Bottlenecks at 100x Scale
* **Load-Bearing `user_id` Index:** Fetching "all tasks assigned to User X across projects" relies critically on `idx_task_assignees_user_id`. Without it, queries would fail because the unique constraint only creates an implicit index composite-keyed on `(task_id, user_id)` starting with `task_id`.
* **High-Volume Join Overhead:** Aggregating global task dashboards at scale requires joining millions of `task_assignees` rows back to `tasks` and `projects`, putting heavy memory pressure on the database without application-level caching or secondary index tuning.

---

## SCHEMA UPDATE: `tasks`

### Migration
ALTER TABLE tasks ADD COLUMN completed_at TIMESTAMPTZ;

### Purpose
completed_at (TIMESTAMPTZ): Stores the UTC timestamp at which a task was completed. The column is nullable because incomplete tasks do not have a completion timestamp.

### Bottlenecks at 100x Scale
Dashboard aggregations currently load visible tasks into memory and aggregate them using Java streams. This is acceptable for a dashboard summary but would become a bottleneck at significantly larger scale.
