# Architecture

The Cuckoo's Nest is one Spring Boot application with three front doors: a dashboard for the
owner, an MCP endpoint for AI agents, and public tripwires for whoever touches a decoy. Every class
named on this page exists in `src/main/java`; `ArchitectureDocsTests` fails the build if one is
renamed or removed, so the diagrams can't quietly drift from the code.

Design decisions and their reasons are recorded in [`docs/adr`](adr/).

## Components

```mermaid
flowchart LR
    owner([Owner's browser])
    agent([AI agent, e.g. Claude Code])
    stranger([Whoever touches a decoy])

    subgraph app [Spring Boot app]
        direction TB
        subgraph web [Dashboard]
            DashboardController
            PageController
        end
        subgraph mcp [MCP server at /mcp]
            NestMcpTools
            McpDecoyTools
        end
        subgraph trip [Public tripwires]
            TripwireRateLimitFilter --> UrlTripController
            TripwireRateLimitFilter --> CredentialTripController
        end
        subgraph decoy [Decoys]
            DecoyService --> DecoyTypeRegistry
            DecoyService --> MissCache
            DecoyTypeRegistry --> UrlDecoyType
            DecoyTypeRegistry --> CredentialDecoyType
            DecoyTypeRegistry --> McpToolDecoyType
        end
        subgraph alert [Alerts]
            AlertService --> RepeatFilter
        end
    end

    owner -- "login (SecurityConfig)" --> web
    agent -- "bearer token (McpSecurityConfig)" --> mcp
    stranger --> trip

    web --> DecoyService
    web --> AlertService
    NestMcpTools --> DecoyService
    NestMcpTools --> AlertService
    McpDecoyTools --> DecoyService
    McpDecoyTools --> AlertService
    UrlTripController --> DecoyService
    UrlTripController --> AlertService
    CredentialTripController --> DecoyService
    CredentialTripController --> AlertService

    DecoyService --> pg[(PostgreSQL)]
    AlertService --> pg
    MissCache --> redis[(Redis)]
    RepeatFilter --> redis
    TripwireRateLimitFilter --> redis
```

| Package | Responsibility |
|---|---|
| `decoy` | Decoys and the plugin system: `DecoyType`, `DecoyTypeRegistry`, `DecoyService`, `TokenGenerator`, `MissCache` |
| `decoy.types` | The three plugins: `UrlDecoyType`, `CredentialDecoyType`, `McpToolDecoyType` |
| `alert` | Recording touches: `AlertService`, `RepeatFilter`, `AlertRepository` |
| `trip` | Public tripwires: `UrlTripController`, `CredentialTripController`, `TripwireRateLimitFilter` |
| `mcp` | The MCP server: `NestMcpTools` (real tools) and `McpDecoyTools` (decoy tools) |
| `web` | The dashboard: `DashboardController`, `PageController` |
| `config` | Security, settings, i18n: `SecurityConfig`, `McpSecurityConfig`, `AdminUserConfig`, `NestProperties`, `WebConfig` |

## The plugin system

A new kind of decoy is one class that implements `DecoyType`. `DecoyTypeRegistry` collects every
plugin bean at startup, so nothing else changes (see [ADR 2](adr/0002-decoys-are-plugins.md)).

```mermaid
classDiagram
    class DecoyType {
        <<interface>>
        +id() String
        +displayName() String
        +newToken(TokenGenerator) String
        +bait(Decoy, String baseUrl) String
    }
    class DecoyTypeRegistry {
        +find(String id) Optional~DecoyType~
        +get(String id) DecoyType
        +all() Collection~DecoyType~
    }
    class UrlDecoyType {
        bait: https://nest/t/token
    }
    class CredentialDecoyType {
        token: nest_ + 30 chars + CRC32 checksum
        +isWellFormed(String)$ boolean
    }
    class McpToolDecoyType {
        token: a tempting tool name
    }
    DecoyType <|.. UrlDecoyType
    DecoyType <|.. CredentialDecoyType
    DecoyType <|.. McpToolDecoyType
    DecoyTypeRegistry o-- DecoyType : every plugin bean
    DecoyService --> DecoyTypeRegistry
    DecoyService --> TokenGenerator
```

## A URL decoy is touched

```mermaid
sequenceDiagram
    autonumber
    actor S as Stranger
    participant C as Caddy (HTTPS)
    participant F as TripwireRateLimitFilter
    participant U as UrlTripController
    participant D as DecoyService
    participant M as MissCache (Redis)
    participant DB as PostgreSQL
    participant A as AlertService
    participant R as RepeatFilter (Redis)

    S->>C: GET /t/{token}
    C->>F: forwards, with X-Forwarded-For
    F->>F: INCR per IP per minute (over the limit: 429)
    F->>U: request
    U->>D: findActive(URL, token)
    D->>M: isKnownMiss?
    alt cached miss (scanner noise)
        M-->>D: yes, so no database query
    else not cached
        D->>DB: find by token
        DB-->>D: decoy, or nothing (then remember the miss)
    end
    opt live decoy
        U->>A: recordTouch(decoy, HTTP, ip, user agent, details)
        A->>R: first touch from this source in 60 s?
        R-->>A: yes
        A->>DB: insert alert
        A->>A: log WARN "decoy.touched" with key-value fields
    end
    U-->>S: 404 Not Found (always the same answer)
```

## An AI agent calls a decoy tool

```mermaid
sequenceDiagram
    autonumber
    actor G as AI agent
    participant S as McpSecurityConfig
    participant T as McpDecoyTools
    participant D as DecoyService
    participant A as AlertService

    G->>S: POST /mcp tools/list (Authorization: Bearer token)
    S-->>G: real tools + decoy tools, e.g. get_production_credentials_v2
    G->>S: tools/call get_production_credentials_v2
    S->>T: authenticated call
    T->>D: findActive(MCP_TOOL, name)
    D-->>T: live decoy
    T->>A: recordTouch(decoy, MCP, client name/version, arguments)
    T-->>G: error: "Permission denied: requires an approved change ticket."
```

When a decoy tool is created or retired, `DecoyService` publishes a `DecoyEvent`, and
`McpDecoyTools` adds or removes the tool on the live MCP server after the database commit.

## Data model

```mermaid
erDiagram
    DECOY ||--o{ ALERT : "is touched by"
    DECOY {
        uuid id PK
        varchar type "plugin id: URL, CREDENTIAL, MCP_TOOL"
        varchar token UK "the secret part"
        varchar label
        varchar location "where it was planted"
        timestamptz created_at
        timestamptz retired_at "null while active"
    }
    ALERT {
        bigserial id PK
        uuid decoy_id FK
        timestamptz touched_at
        varchar channel "HTTP or MCP"
        varchar source_ip
        varchar user_agent
        jsonb details
    }
```

Flyway owns the schema (`src/main/resources/db/migration`); Hibernate only validates that the
entities match it ([ADR 5](adr/0005-flyway-owns-the-schema.md)).

## Deployment

```mermaid
flowchart LR
    dev([Pull request]) --> ci[GitHub Actions: build, test, lint, CodeQL, PIT]
    ci -- "green build on main" --> ghcr[(GitHub Container Registry)]

    subgraph ec2 [AWS EC2, t2.micro]
        timer[nest-update.timer, every 2 min] -- docker compose pull --> app
        caddy[Caddy: HTTPS, Let's Encrypt] --> app[app container]
        app --> redis[(Redis, cache only)]
        backup[nest-backup.timer, nightly pg_dump]
    end

    ghcr -. pulled by .-> timer
    users([Internet]) -- 443 --> caddy
    app -- TLS --> rds[(Amazon RDS PostgreSQL)]
    backup --> rds
```

The server pulls new images; GitHub never pushes into it
([ADR 8](adr/0008-pull-based-continuous-deployment.md)). Details: [deploy/README.md](../deploy/README.md).
