CREATE TABLE tasks (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT       NOT NULL REFERENCES projects(id),
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    priority            VARCHAR(10)  NOT NULL,
    due_date            TIMESTAMPTZ,
    status              VARCHAR(20)  NOT NULL,
    blocked_from_status VARCHAR(20),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_tasks_status CHECK (status IN ('BACKLOG', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'BLOCKED')),
    CONSTRAINT chk_tasks_blocked_from_status CHECK (blocked_from_status IN ('IN_PROGRESS', 'IN_REVIEW'))
);

CREATE INDEX idx_tasks_project_id ON tasks (project_id);