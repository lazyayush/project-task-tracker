CREATE TABLE task_assignees (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT      NOT NULL REFERENCES tasks(id),
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_task_assignees_task_user UNIQUE (task_id, user_id)
);

CREATE INDEX idx_task_assignees_user_id ON task_assignees (user_id);