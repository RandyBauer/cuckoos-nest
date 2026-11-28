# 8. Continuous deployment pulls; GitHub never pushes into the server

- **Status:** Accepted
- **Date:** 2026-11-28

## Context

The usual CD pattern has GitHub Actions SSH into the server after a green build. That requires opening SSH to GitHub's large, changing IP ranges and storing a private deploy key in GitHub, where any workflow change could misuse it.

## Decision

CI publishes a Docker image to GitHub's container registry only after every check passes on `main`. On the server, a systemd timer pulls the image every two minutes and recreates the app only if the image changed.

## Consequences

- SSH stays restricted to the owner's IP address, and no cloud credentials or deploy keys live in GitHub.
- A deploy takes up to two minutes after the image is published, rather than seconds.
- Each image is labeled with its commit, so `docker image inspect` shows exactly what is live.
