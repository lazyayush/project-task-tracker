CREATE TABLE alert_dismissals (
    id           BIGSERIAL PRIMARY KEY,
    task_id      BIGINT      NOT NULL REFERENCES tasks(id),
    user_id      BIGINT      NOT NULL REFERENCES users(id),
    dismissed_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_alert_dismissals_task_user UNIQUE (task_id, user_id)
);