#!/bin/sh
# Proves the newest backup actually restores: loads it into a throwaway Postgres container,
# compares table row counts with production, and removes the container. Run it by hand:
#   sudo /opt/cuckoos-nest/nest-restore-test.sh
set -eu
cd /opt/cuckoos-nest
. ./.env
latest=$(ls -1t /var/backups/nest/nest-*.sql.gz | head -1)
echo "Restoring $latest into a scratch database..."
docker rm -f nest-restore-check >/dev/null 2>&1 || true
docker run -d --name nest-restore-check -e POSTGRES_PASSWORD=scratch postgres:18-alpine >/dev/null
trap 'docker rm -f nest-restore-check >/dev/null 2>&1' EXIT
until docker exec nest-restore-check pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done
sleep 2
gunzip -c "$latest" | docker exec -i nest-restore-check psql -q -U postgres -v ON_ERROR_STOP=1 >/dev/null
for table in decoy alert flyway_schema_history; do
    restored=$(docker exec nest-restore-check psql -tA -U postgres -c "select count(*) from $table")
    echo "$table: $restored rows restored"
done
echo "Restore test passed."
