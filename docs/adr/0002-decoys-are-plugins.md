# 2. Decoys are plugins

- **Status:** Accepted
- **Date:** 2026-11-28

## Context

The Nest started with URL decoys and was always meant to grow: fake API keys, fake AI tools, and later fake files. If each new kind touched the services, controllers, and dashboard, every addition would risk breaking the others.

## Decision

Each kind of decoy is a Spring bean implementing `DecoyType` (id, display name, token generator, bait). `DecoyTypeRegistry` collects all of them at startup and rejects duplicate or malformed ids. The database stores the plugin id as a plain string.

## Consequences

- A new kind of decoy is one class plus its tests; the dashboard, MCP tools, and services pick it up automatically (open/closed principle).
- `DecoyTypeContractTests` checks every plugin against the same rules.
- Tripwire controllers are still per kind, because each kind is "touched" differently (a URL request, an API call, a tool call).
