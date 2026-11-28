-- Sample data for local development only. Flyway loads this folder only under the "dev" profile.
-- Repeatable migration (R__): Flyway re-runs it whenever this file changes.
INSERT INTO decoy (id, type, token, label, location, created_at)
VALUES
    ('00000000-0000-4000-8000-000000000001', 'URL', 'dev-admin-dashboard',
     'Fake internal admin dashboard', 'docs/OPERATIONS.md, "Admin" section', now() - interval '3 days'),
    ('00000000-0000-4000-8000-000000000002', 'CREDENTIAL', 'nest_devexamplekeynotreal00000000000Xw0Rp',
     'Fake payments API key', 'config/.env.production.example', now() - interval '2 days'),
    ('00000000-0000-4000-8000-000000000003', 'MCP_TOOL', 'get_production_credentials',
     'Tempting MCP tool', 'Listed next to the real MCP tools', now() - interval '1 day')
ON CONFLICT (id) DO UPDATE
    SET token = EXCLUDED.token, label = EXCLUDED.label, location = EXCLUDED.location;

INSERT INTO alert (decoy_id, touched_at, channel, source_ip, user_agent, details)
SELECT '00000000-0000-4000-8000-000000000001', now() - interval '5 hours', 'HTTP',
       '203.0.113.7', 'curl/8.9.1', '{"method": "GET", "path": "/t/dev-admin-dashboard"}'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM alert WHERE decoy_id = '00000000-0000-4000-8000-000000000001');
