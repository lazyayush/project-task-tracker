CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    project_key VARCHAR(20)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id    BIGINT       NOT NULL REFERENCES users(id),
    archived    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_projects_key UNIQUE (project_key)
);