# 5. Flyway owns the schema; Hibernate only validates

- **Status:** Accepted
- **Date:** 2026-11-28

## Context

Letting Hibernate create or update tables (`ddl-auto=update`) is convenient but unpredictable in production: it can't rename columns, it never drops anything, and the actual schema ends up defined by whatever version of the code ran last.

## Decision

All schema changes are versioned SQL migrations under `src/main/resources/db/migration`, applied by Flyway at startup. Hibernate runs with `ddl-auto=validate`, so the app refuses to start if the entities and the schema disagree. Dev-only seed data lives in a separate repeatable migration loaded only by the `dev` profile.

## Consequences

- The schema is reviewable in pull requests like any other code.
- A migration that has run is never edited; fixes are new migrations.
- Tests run the real migrations against a real Postgres (Testcontainers).
