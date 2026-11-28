# MCP API reference

The Nest's API for AI agents is a [Model Context Protocol](https://modelcontextprotocol.io) server.
Every tool on this page is checked by `McpDocsTests`: the build fails if a tool or parameter is
added to the code without being documented here.

| | |
|---|---|
| Endpoint | `https://<nest>/mcp` (live: `https://54-186-140-252.sslip.io/mcp`) |
| Transport | Streamable HTTP, JSON-RPC 2.0 messages |
| Auth | `Authorization: Bearer <NEST_MCP_TOKEN>`; anything else gets `401` with `WWW-Authenticate: Bearer` |
| Server info | name `cuckoos-nest`, version `0.1.0` |

## Connect

Claude Code:

```bash
claude mcp add --transport http cuckoos-nest https://<nest>/mcp \
  --header "Authorization: Bearer <NEST_MCP_TOKEN>"
```

Any MCP client works the same way. The raw protocol, with `curl`:

```bash
# 1. Start a session. The response header Mcp-Session-Id identifies it.
curl -i https://<nest>/mcp \
  -H "Authorization: Bearer $NEST_MCP_TOKEN" \
  -H "Content-Type: application/json" -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",
       "capabilities":{},"clientInfo":{"name":"curl","version":"1"}}}'

# 2. List the tools (send the Mcp-Session-Id header from step 1 on every later request).
curl https://<nest>/mcp -H "Authorization: Bearer $NEST_MCP_TOKEN" -H "Mcp-Session-Id: <id>" \
  -H "Content-Type: application/json" -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
```

`tools/list` also returns each tool's JSON Schema, so clients discover the parameters below on
their own.

## Tools

### `create_decoy`

Creates a decoy and returns the bait to plant.

| Parameter | Type | Required | Meaning |
|---|---|---|---|
| `type` | string | yes | `URL`, `CREDENTIAL`, or `MCP_TOOL` (case-insensitive) |
| `label` | string | yes | Your name for it, up to 200 characters |
| `location` | string | no | Where you will plant it, up to 500 characters |

Returns a **decoy**:

```json
{"id":"3f0c…","type":"URL","label":"Fake admin page in README","location":"README.md",
 "active":true,"createdAt":"2026-12-01T12:00:00Z","bait":"https://<nest>/t/8kQ2…"}
```

The bait depends on the type: a link for `URL`; an `API_BASE_URL=…` and `API_KEY=nest_…` pair for
`CREDENTIAL`; and for `MCP_TOOL`, a note naming the new tool, which appears on this server right away.

### `list_decoys`

| Parameter | Type | Required | Meaning |
|---|---|---|---|
| `includeRetired` | boolean | no | Include retired decoys (default `false`) |

Returns a list of decoys, newest first, in the same shape as `create_decoy`.

### `list_alerts`

| Parameter | Type | Required | Meaning |
|---|---|---|---|
| `hours` | integer | no | How far back to look (default 24) |
| `limit` | integer | no | Maximum alerts (default 50, at most 500) |

Returns a list of **alerts**, newest first:

```json
{"id":42,"decoyId":"3f0c…","decoyLabel":"Fake admin page in README","decoyType":"URL",
 "touchedAt":"2026-12-01T12:05:00Z","channel":"HTTP","sourceIp":"203.0.113.7",
 "userAgent":"curl/8.9.1","details":{"method":"GET","path":"/t/8kQ2…"}}
```

`channel` is `HTTP` for links and keys, and `MCP` for decoy tools (then `userAgent` holds the
calling client's name and version).

### `explain_alert`

| Parameter | Type | Required | Meaning |
|---|---|---|---|
| `alertId` | integer | yes | An `id` from `list_alerts` |

Returns plain text: what was touched, where it was planted, who touched it, and what to check next.

### `retire_decoy`

| Parameter | Type | Required | Meaning |
|---|---|---|---|
| `decoyId` | string | yes | An `id` from `list_decoys` |

Returns the retired decoy. Touches after this record nothing.

## Decoy tools

Each active `MCP_TOOL` decoy is also a tool on this server, such as
`get_production_credentials_v2`, with a tempting description and an `environment` parameter. A
well-behaved agent never calls one. A call records an alert (channel `MCP`) and returns an error
result: `Permission denied: this operation requires an approved change ticket.`

## Errors

| Situation | Result |
|---|---|
| Missing or wrong bearer token | HTTP `401`, body `{"error":"unauthorized"}` |
| Bad input (unknown type, blank label, unknown id) | A tool result with `isError: true` and a message naming the problem |

## Why there is no OpenAPI / Swagger page

The app's other HTTP endpoints are the public tripwires (`/t/…` and the decoy API under
`/api/v1/…`). Publishing them in a Swagger UI would hand an intruder a map of the traps, which
defeats them (see [ADR 3](adr/0003-tripwires-answer-identically.md)). The dashboard serves HTML
pages, not an API. So the documented API is MCP, and the code itself is documented with Javadoc,
published at https://randybauer.github.io/cuckoos-nest/.
