# 4. One decoy is planted in exactly one place

- **Status:** Accepted
- **Date:** 2026-11-28

## Context

The first design had a separate `plant` table so one decoy could be planted in several places. But then a touch couldn't tell you *which* place was visited, which is the most useful fact an alert can carry.

## Decision

A decoy has a single `location` column. To bait three places, create three decoys. Tokens are cheap.

## Consequences

- Every alert points to exactly one planted location.
- Simpler schema and queries; issue #6 was updated to record this change.
