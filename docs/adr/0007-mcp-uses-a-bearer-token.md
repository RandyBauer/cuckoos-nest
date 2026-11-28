# 7. The MCP endpoint uses a bearer token, not a login session

- **Status:** Accepted
- **Date:** 2026-11-28

## Context

AI agents connect to `/mcp` from tools like Claude Code. They can't fill in a login form or keep cookies, and they should not share the human's dashboard session.

## Decision

`/mcp` has its own security chain: stateless, no sessions, no cookies, and access only with `Authorization: Bearer <NEST_MCP_TOKEN>`, compared in constant time. With no token configured, the endpoint refuses everyone. CSRF protection is off for this chain only, because CSRF works by riding credentials the browser attaches automatically, and a bearer header is never attached automatically.

## Consequences

- CodeQL flags the disabled CSRF and the token check; both were reviewed and dismissed as false positives with this reasoning, which is also in code comments.
- One shared token means all agents are equal; per-agent tokens would be the next step if needed.
