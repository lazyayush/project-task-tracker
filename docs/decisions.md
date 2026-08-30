# Decisions

## Decision 1

- **Chose:** Local PostgreSQL via Docker Compose paired with Flyway migrations (ddl-auto: validate).
- **Rejected:** In-memory H2 database with Hibernate ddl-auto: update.
- **Why:** In-memory H2 with ddl-auto: update was ideal for rapid initial scaffolding, but switching to Dockerized Postgres ensures environment parity with production. Flyway SQL scripts (V1__...sql) provide explicit, version-controlled schema tracking, unlike implicit runtime inference.
- **Later reversed:** Initially planned to use in-memory H2 to avoid local setup overhead, but switched the strategy to Dockerized Postgres and Flyway before implementation to guarantee production parity and strict schema control from day one.