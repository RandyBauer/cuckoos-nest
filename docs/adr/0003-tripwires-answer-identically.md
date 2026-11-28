# 3. Tripwires give the same answer for traps and non-traps

- **Status:** Accepted
- **Date:** 2026-11-28

## Context

A tripwire is useful only while the person touching it doesn't know it's a trap. If a live decoy answered differently from a random URL or wrong key (a different status, body, header, or even rate limit), an intruder could probe for traps and avoid them.

## Decision

Every tripwire returns one fixed response for its kind, whatever the token: URL decoys always answer `404 Not Found`; the decoy API always answers `401` with the same JSON body and `WWW-Authenticate` header. The rate limit applies to all tripwire paths alike.

## Consequences

- Recording an alert happens as a side effect; the response never depends on it.
- Tests assert that live and unknown tokens get byte-identical responses.
- Timing differences remain a theoretical side channel; the miss cache narrows it for repeated probes.
