CREATE TABLE task_blocks (
    id               BIGSERIAL PRIMARY KEY,
    blocking_task_id BIGINT      NOT NULL REFERENCES tasks(id),
    blocked_task_id  BIGINT      NOT NULL REFERENCES tasks(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_task_blocks_pair UNIQUE (blocking_task_id, blocked_task_id),
    CONSTRAINT chk_task_blocks_not_self CHECK (blocking_task_id != blocked_task_id)
);

CREATE INDEX idx_task_blocks_blocked_task_id ON task_blocks (blocked_task_id);