# The Cuckoo's Nest

[![CI](https://github.com/RandyBauer/cuckoos-nest/actions/workflows/ci.yml/badge.svg)](https://github.com/RandyBauer/cuckoos-nest/actions/workflows/ci.yml)

Honeytokens that catch whoever touches them — AI agents included.

A honeytoken is a decoy: a link, a fake API key, or a fake tool that nobody has any reason to
touch. So any touch at all is an alarm. The name comes from Cliff Stoll's *The Cuckoo's Egg*,
where a fake set of files was the bait that caught a real intruder.

The Nest plants decoys where people and coding agents wander — docs, config files, and the tool
list an agent sees over the [Model Context Protocol](https://modelcontextprotocol.io) — and
records every touch.

> **Status:** under construction. See the [v1.0 milestone](https://github.com/RandyBauer/cuckoos-nest/milestone/1).

## Tech stack

Java 21 · Spring Boot 4.2 · Spring AI MCP server · PostgreSQL · Redis · Gradle

## Build and run

You need **JDK 21** and **Docker** (the tests start real Postgres and Redis containers).

```bash
# Compile, run all tests, and run the checks
./gradlew build

# Start the app on http://localhost:8080 with throwaway Postgres and Redis containers
./gradlew bootTestRun
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

`./gradlew build` also runs the code-quality checks, and fails if either one finds a problem:

- **Spotless** checks formatting (Palantir Java Format). Run `./gradlew spotlessApply` to fix it.
- **Checkstyle** checks naming and common mistakes. The rules are in
  [`config/checkstyle/checkstyle.xml`](config/checkstyle/checkstyle.xml).

Check that it is up:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## Contributing

[CI](.github/workflows/ci.yml) runs `./gradlew build` on every push to `main` and on every pull
request. `main` is protected: changes arrive only through pull requests, and CI must pass first.

Work happens on a branch named for its issue, for example `1-project-skeleton`. Every change
reaches `main` through a pull request that is reviewed against the
[checklist](.github/pull_request_template.md) and squash-merged with a title that ends in
`Closes #<issue>`.
