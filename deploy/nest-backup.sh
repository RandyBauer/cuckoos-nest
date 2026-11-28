#!/bin/sh
# Nightly logical backup of the production database (run by nest-backup.timer). This is on top of
# Amazon RDS's own automated snapshots: a plain SQL dump can be restored anywhere, by anyone.
# Keeps the newest 14 dumps in /var/backups/nest.
set -eu
cd /opt/cuckoos-nest
. ./.env
dir=/var/backups/nest
mkdir -p "$dir"
file="$dir/nest-$(date -u +%Y%m%d-%H%M%S).sql.gz"
# DATABASE_URL is jdbc:postgresql://HOST:PORT/DB; pg_dump wants HOST, PORT, and DB separately.
hostportdb=${DATABASE_URL#jdbc:postgresql://}
host=${hostportdb%%:*}
portdb=${hostportdb#*:}
port=${portdb%%/*}
db=${portdb#*/}
docker run --rm -e PGPASSWORD="$DATABASE_PASSWORD" postgres:18-alpine \
    pg_dump --host "$host" --port "$port" --username "$DATABASE_USERNAME" --dbname "$db" \
    --no-owner --no-privileges | gzip > "$file.tmp"
mv "$file.tmp" "$file"
ls -1t "$dir"/nest-*.sql.gz | tail -n +15 | xargs -r rm -f
echo "Backup written: $file ($(du -h "$file" | cut -f1))"
