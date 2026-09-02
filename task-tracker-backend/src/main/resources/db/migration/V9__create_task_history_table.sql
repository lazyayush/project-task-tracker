CREATE TABLE task_history (
    id           BIGSERIAL PRIMARY KEY,
    task_id      BIGINT       NOT NULL REFERENCES tasks(id),
    actor_id     BIGINT       NOT NULL REFERENCES users(id),
    event_type   VARCHAR(20)  NOT NULL,
    field_name   VARCHAR(50),
    old_value    TEXT,
    new_value    TEXT,
    comment_text TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_task_history_event_type
        CHECK (event_type IN ('CREATED', 'FIELD_CHANGED', 'ASSIGNED', 'UNASSIGNED', 'COMMENT'))
);

CREATE INDEX idx_task_history_task_id ON task_history (task_id);

-- Immutability, enforced at the database, not just by application convention.
CREATE OR REPLACE FUNCTION prevent_task_history_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'task_history rows are immutable — % is not permitted', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_task_history_no_update
    BEFORE UPDATE ON task_history
    FOR EACH ROW EXECUTE FUNCTION prevent_task_history_mutation();

CREATE TRIGGER trg_task_history_no_delete
    BEFORE DELETE ON task_history
    FOR EACH ROW EXECUTE FUNCTION prevent_task_history_mutation();