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
