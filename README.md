# Dulo Server

Dulo is a self-hosted app for managing family trees. This repository holds the **server**: a REST
API used by the Dulo web client. Users sign in through an identity provider you already use, such
as Google, Microsoft or a self-hosted one like Pocket ID.

> **Status:** early development. The foundation (build, configuration, database support) is in
> place; sign-in, the initial setup and family tree features are coming next.

**Docs:** [rdlf0.github.io/dulo-server](https://rdlf0.github.io/dulo-server/): REST API reference
(Swagger UI), the OpenAPI document for generating clients, the Javadoc, and test and coverage reports.

## Quick start

Dulo is distributed as one Docker image with the server and the web client, built by the web
client project. This repository releases the server JAR that the image runs.

To run the server on its own, download the JAR from the
[latest release](https://github.com/rdlf0/dulo-server/releases/latest) and start it with Java 25.
It stores a SQLite database in `./data`:

```sh
java -jar dulo-server-X.Y.Z.jar
```

Check that it is up:

```sh
curl http://localhost:8080/actuator/health
```

On first start, and until the initial setup is done, the server logs its merged configuration.
Each value shows where it came from (environment variable, config file or default), and secrets
are masked.

## Configuration

Settings come from three places, in increasing order of precedence:

1. Built-in defaults
2. A YAML file, `/config/dulo.yaml` by default (change the path with `DULO_CONFIG_FILE`)
3. Environment variables

See [`deploy/dulo.example.yaml`](deploy/dulo.example.yaml) for all options. Point your editor at
the [JSON Schema](schema/dulo-config.schema.json) for completion and validation; the example file
shows how.

Environment variable names are the property path in upper case, with dots replaced by `_` and
dashes removed:

| Property                                     | Environment variable                         |
|----------------------------------------------|----------------------------------------------|
| `dulo.database.url`                          | `DULO_DATABASE_URL`                          |
| `dulo.identity.providers.google.client-id`   | `DULO_IDENTITY_PROVIDERS_GOOGLE_CLIENTID`    |
| `dulo.features.map-view`                     | `DULO_FEATURES_MAP_VIEW`                     |

### Database

SQLite in the data directory (`./data` by default, set with `DULO_DATA_DIR`) is the default. For PostgreSQL, MySQL or MariaDB
set the JDBC URL and credentials:

```sh
DULO_DATABASE_URL=jdbc:postgresql://db:5432/dulo
DULO_DATABASE_USERNAME=dulo
DULO_DATABASE_PASSWORD=...
```

The schema is created and upgraded automatically on start.

### Features

Features can be forced on or off with `dulo.features.<feature>: true|false`. Forced features are
locked in the administration page; the others are managed by the administrator. Available
features: `people`, `families`, `events`, `places`, `tree-view`, `map-view`, `timeline`,
`audit-log`.

## Development

Requirements: JDK 25 and Docker (integration tests use Testcontainers).

```sh
./mvnw verify                                       # format check, unit, architecture and integration tests
./mvnw com.spotify.fmt:fmt-maven-plugin:format      # format the code (Google Java Format, AOSP style)
java -jar dulo-service/target/dulo-server-*.jar     # run locally, data in ./data
```

The project follows a hexagonal architecture:

| Module                         | Contents                                                                       |
|--------------------------------|--------------------------------------------------------------------------------|
| `dulo-domain`                  | Domain model. Plain Java; its only dependency is JSpecify (null annotations).  |
| `dulo-core`                    | Use cases and ports. Depends only on `dulo-domain`.                            |
| `dulo-adapter-persistence-jpa` | Database adapter: JPA and Liquibase for SQLite, PostgreSQL, MySQL and MariaDB. |
| `dulo-adapter-rest`            | REST API adapter.                                                              |
| `dulo-service`                 | Spring Boot application that wires everything together.                        |

Architecture rules are checked by ArchUnit tests in `dulo-service`.

The OpenAPI document is checked in at [`api/openapi.json`](api/openapi.json), and a running
server also serves it at `/api/v1/openapi.json`.

See [CONTRIBUTING.md](CONTRIBUTING.md) for the workflow.
