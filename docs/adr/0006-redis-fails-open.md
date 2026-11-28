# 6. Redis features fail open

- **Status:** Accepted
- **Date:** 2026-11-28

## Context

Redis serves three jobs on the alarm path: a cache of tokens that are not decoys, collapsing bursts of repeated touches, and rate limiting the public tripwires. Redis is a cache, not the system of record, and it can go down.

## Decision

Every Redis call catches data-access errors and falls back to the behavior with no cache: look the token up in Postgres, record every touch, let the request through. Each fallback logs a warning.

## Consequences

- An outage of Redis can never cause a lost alarm, which is the one thing an alarm system must not do.
- During an outage, Postgres takes more load and duplicate alerts are possible; both are preferable to silence.
- `RedisDownTests` simulates the outage for all three features.
