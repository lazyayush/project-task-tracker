# Schema

## Table: `users`

### Columns & Data Types
* **`id`** (`BIGSERIAL`): 64-bit auto-incrementing primary key.
* **`email`** (`VARCHAR(255)`): Unique user email address.
* **`password_hash`** (`VARCHAR(255)`): Hashed credentials.
* **`role`** (`VARCHAR(20)`): User authorization level (`MANAGER` or `MEMBER`).
* **`created_at`** (`TIMESTAMPTZ`): Record creation timestamp in UTC.

### Relationships
* **None.** This single table has no foreign keys or relationships to other entities.

### Constraints & Architecture Boundaries
* **Database-enforced:** `PRIMARY KEY`, `NOT NULL`, `uq_users_email` (uniqueness), `chk_users_role` (allowed role values). Regradless of input source it guarantees absolute data integrity.
* **Application-enforced:** Password hashing algorithms, email string patterns, and workflow authorization.

### Deliberate Denormalization
* **Inline `role` column:** Stored directly as a string instead of using a separate `roles` lookup table with foreign keys to avoid redundant `JOIN` operations during authentication.

### Bottlenecks at 100x Scale
* **Unindexed reads:** Searching by `role` or sorting by `created_at` will trigger expensive full table scans without secondary indexes.
* **Storage & RAM footprint:** Disk space and buffer pool RAM usage will spike due to large `VARCHAR(255)` columns and growing primary key / unique indexes.