-- A decoy is something nobody has a reason to touch. Each one is planted in exactly one place,
-- so a touch tells you which place was visited.
CREATE TABLE decoy (
    id          UUID PRIMARY KEY,
    type        VARCHAR(32)  NOT NULL,              -- plugin id, e.g. URL, CREDENTIAL, MCP_TOOL
    token       VARCHAR(128) NOT NULL UNIQUE,       -- the secret part: URL path, fake key, tool name
    label       VARCHAR(200) NOT NULL,              -- what the owner calls it
    location    VARCHAR(500),                       -- where it was planted, in the owner's words
    created_at  TIMESTAMPTZ  NOT NULL,
    retired_at  TIMESTAMPTZ                         -- set when the decoy is taken out of service
);

-- One row per touch. Alerts are append-only: the app never updates or deletes them.
CREATE TABLE alert (
    id          BIGSERIAL    PRIMARY KEY,
    decoy_id    UUID         NOT NULL REFERENCES decoy (id),
    touched_at  TIMESTAMPTZ  NOT NULL,
    channel     VARCHAR(32)  NOT NULL,              -- how it was touched: HTTP or MCP
    source_ip   VARCHAR(64),
    user_agent  VARCHAR(500),
    details     JSONB        NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX alert_decoy_touched_idx ON alert (decoy_id, touched_at DESC);
CREATE INDEX alert_touched_idx ON alert (touched_at DESC);
