# Deployment

Production runs on one AWS EC2 instance (t2.micro, Ubuntu 24.04) with the database on Amazon RDS.

```
Internet ──https──▶ Caddy (80/443, Let's Encrypt) ──▶ app (Spring Boot) ──▶ PostgreSQL on Amazon RDS
                                                                   └──▶ Redis (cache only)
```

## Continuous deployment

1. **CI** ([ci.yml](../.github/workflows/ci.yml)) runs every test on each push to `main`.
2. Only if the build is green, the **publish image** job pushes `ghcr.io/randybauer/cuckoos-nest`,
   tagged `latest` and with the commit SHA.
3. On the server, **`nest-update.timer`** runs [`nest-update.sh`](nest-update.sh) every two
   minutes. It pulls `latest` and recreates the app only if the image changed.

The server pulls; GitHub never pushes into it. SSH stays locked to the owner's IP, and no deploy
keys or cloud credentials are stored in GitHub. To see which commit is live:

```bash
docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' \
  ghcr.io/randybauer/cuckoos-nest:latest
```

## Backups

- **Amazon RDS automated backups:** daily snapshots with point-in-time restore, managed by AWS.
- **Nightly SQL dump:** `nest-backup.timer` runs [`nest-backup.sh`](nest-backup.sh) at 09:30 UTC
  and keeps 14 gzipped dumps in `/var/backups/nest`. A plain dump restores anywhere.
- **Restore test:** [`nest-restore-test.sh`](nest-restore-test.sh) loads the newest dump into a
  throwaway Postgres and counts the rows. Run it after any change to backups.

## Setting up a fresh server

```bash
sudo mkdir -p /opt/cuckoos-nest && cd /opt/cuckoos-nest
# Copy docker-compose.prod.yml, Caddyfile, and the three scripts from deploy/ here.
sudo cp .env.sample .env && sudo chmod 600 .env   # then fill it in; see ../.env.sample
sudo cp systemd/* /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now nest-update.timer nest-backup.timer
docker compose -f docker-compose.prod.yml up -d
```

In `.env`, wrap each value in single quotes, so `$` in the bcrypt hash isn't read as a variable.
Set `NEST_HOST` to the public name for HTTPS (here `<ip-with-dashes>.sslip.io`, a free DNS name
that resolves to the IP) and `NEST_PUBLIC_BASE_URL` to `https://` plus that name.
