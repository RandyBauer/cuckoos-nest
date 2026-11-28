#!/bin/sh
# Continuous deployment, step 2 of 2 (step 1 is the "publish image" job in .github/workflows/ci.yml).
# Run every two minutes by nest-update.timer. CI only publishes an image after every test passes,
# so this deploys only green builds. "up -d" recreates the app only if the pulled image changed.
set -eu
cd /opt/cuckoos-nest
before=$(docker image inspect --format '{{.Id}}' ghcr.io/randybauer/cuckoos-nest:latest 2>/dev/null || echo none)
docker compose -f docker-compose.prod.yml pull --quiet app
after=$(docker image inspect --format '{{.Id}}' ghcr.io/randybauer/cuckoos-nest:latest)
if [ "$before" != "$after" ]; then
    revision=$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' ghcr.io/randybauer/cuckoos-nest:latest)
    echo "New image for commit ${revision:-unknown}; deploying."
    docker compose -f docker-compose.prod.yml up -d app
    docker image prune -f >/dev/null
else
    echo "No new image."
fi
