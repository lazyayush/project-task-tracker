CREATE TABLE project_members (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT      NOT NULL REFERENCES projects(id),
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_project_members_project_user UNIQUE (project_id, user_id)
);