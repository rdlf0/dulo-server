# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Dulo is a self-hosted family tree app. This repository is the **server**: a Spring Boot 4 REST API
on Java 25, built with the Maven Wrapper. The web client lives in a separate repository, which also
builds the single Docker image (server + web client). This repository only releases the server JAR.

## Commands

```sh
./mvnw verify                                     # format check, unit, ArchUnit and integration tests (needs Docker)
./mvnw com.spotify.fmt:fmt-maven-plugin:format    # fix formatting; the build fails on unformatted code
./mvnw verify site site:stage                     # docs site in target/staging (Swagger UI, Javadoc, reports)
java -jar dulo-service/target/dulo-server-*.jar   # run locally, SQLite in ./data
```

Running one test. Use `-pl <module> -am`, because the enforcer's `reactorModuleConvergence` rule
fails a build with `-pl` alone:

```sh
# Unit test
./mvnw -pl dulo-service -am test -Dtest=ConfigPrecedenceTest -Dsurefire.failIfNoSpecifiedTests=false

# Integration test (*IT, run by Failsafe). PostgreSQL, MySQL and MariaDB ITs use Testcontainers.
./mvnw -pl dulo-service -am verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=SqliteTreeApiIT -Dfailsafe.failIfNoSpecifiedTests=false
```

The build also runs `maven-dependency-plugin:analyze-only` with `failOnWarning`, so every directly
used dependency must be declared, and unused ones fail the build.

### Checked-in generated files

Tests fail when these are out of date. Regenerate them, don't edit them by hand:

- `schema/dulo-config.schema.json` (from `DuloProperties`):
  `./mvnw -pl dulo-service -am test -Dtest=ConfigSchemaTest -Dsurefire.failIfNoSpecifiedTests=false -Ddulo.schema.update=true`
- `api/openapi.json` (from the running app's springdoc output):
  `./mvnw -pl dulo-service -am test -Dtest=OpenApiDocumentTest -Dsurefire.failIfNoSpecifiedTests=false -Ddulo.openapi.update=true`

## Architecture

Hexagonal (onion) layout, one Maven module per layer. Packages are `com.dulo.<layer>...`:

- `dulo-domain`: the domain model in plain Java (records, value objects, `Ids` for UUIDv7). Its only
  dependency is JSpecify.
- `dulo-core`: use cases and ports, with no Spring. It is organized per context:
  - `port.in`: use case interfaces, such as `GetTreeUseCase`;
  - `port.out`: what the core needs, such as `TreeSettingsRepository`, `FeatureOverridesPort` and
    the shared `UnitOfWork`;
  - `service`: the implementations.

  Transactions go through the `UnitOfWork` port, not `@Transactional`.
- `dulo-adapter-persistence-jpa`: implements the outbound ports with JPA, Hibernate and Liquibase.
  One adapter covers SQLite, PostgreSQL and MySQL/MariaDB. The changelogs are in
  `src/main/resources/db/changelog/`. JPA entities never leave the adapter.
- `dulo-adapter-rest`: Spring MVC controllers that only call `port.in` interfaces. Errors are
  RFC 9457 `ProblemDetail` responses with `urn:dulo:problem:*` types. It also holds the OpenAPI
  setup.
- `dulo-service`: the Spring Boot application, which builds `dulo-server-<version>.jar`. It holds:
  - the `@ConfigurationProperties("dulo")` records (`DuloProperties`);
  - the config report, logged on startup with masked secrets and the source of each value;
  - wiring: core services are plain classes, created as `@Bean`s in `wiring/CoreServicesConfiguration`;
  - all ArchUnit tests and integration tests.

Each adapter has one entry-point `@Configuration` (`PersistenceJpaConfiguration`,
`RestApiConfiguration`), and `DuloApplication` `@Import`s them explicitly. There is no
application-wide component scan. `ArchitectureTest` (with `onionArchitecture()`) enforces the
layers: the domain and core don't depend on frameworks, and adapters don't depend on each other.

### Configuration

Settings come from three places, in increasing order of precedence:

1. `application.yaml` defaults.
2. An optional external YAML file, `/config/dulo.yaml` or the path in `DULO_CONFIG_FILE`.
3. Environment variables.

Feature toggles have three states: when `dulo.features.<feature>` is unset, the administrator
decides (the value is stored in the DB). When it's set, the config forces the value. The core sees
forced values through `FeatureOverridesPort`.

### REST and OpenAPI

- Document endpoints with the Swagger annotations (`@Tag`, `@Operation`, `@ApiResponse`).
- The spec is served at `/api/v1/openapi.json`.
- `RequiredRecordComponentsConverter` marks every response record component as required unless
  it's `@Nullable`.
- The spec's version is the API version (`OpenApiConfiguration.API_VERSION`), not the server
  version.

## Conventions

The full list is in CONTRIBUTING.md ("Code style"), and the IntelliJ settings in `.idea/` define the
rest. The rules that are easy to get wrong:

- **`final`:** every parameter and local is `final`, including catch, for-each, pattern and
  try-with-resources variables.
- **`this.`:** instance calls are `this.`-qualified.
- **Utility classes:** `final`, with a private constructor.
- **Member order:** static fields, instance fields, constructors, static methods, instance
  methods, nested types. Fields go public, protected, package-private, then private.
- **Enums:** the last constant ends with `,`, and `;` goes on its own line.
- **Nullness:** every package has a `package-info.java` with JSpecify's `@NullMarked`, and values
  that may be null get `@Nullable`. Never annotate locals. Test fakes share the main package's
  `@NullMarked`, so mark their nullable fields `@Nullable`.
- **Unused variables:** name them `_`.
- **ArchUnit:** `@ArchTest` fields use CONSTANT_CASE names.
- **Spelling:** American English.
- **POM layout:**
  - dependencies go in `<!-- Internal dependencies -->`, `<!-- 3rd party dependencies -->` and
    `<!-- Test dependencies -->` sections, sorted by groupId, then artifactId;
  - versions, configuration and executions live only in `dependencyManagement` and
    `pluginManagement`;
  - keep a blank line between version properties, so a Dependabot bump changes one line.
- **Plugin versions:** don't set a version for plugins the Spring Boot parent manages. Pin the
  others as properties.
- **Runtime dependencies used in tests:** never make them test-scoped, because that drops them from
  the JAR. Use the `ignoredNonTestScopedDependencies` list in `dulo-service/pom.xml` instead.

## CI and releases

- CI (`.github/workflows/ci.yml`) runs on pull requests only. It runs the build, a JaCoCo coverage
  gate (85% of lines, 70% of branches, from `dulo-service/target/site/jacoco-aggregate`), a JAR
  smoke test, and a site build that fails on Javadoc warnings.
- Releases are started by hand with the `Release` workflow (major, minor or patch). It runs
  `maven-release-plugin` `release:prepare` with a GitHub App token. `release:perform` is never used.
  The workflow then attaches the attested JAR, `api/openapi.json` and the config schema to a GitHub
  release, and calls `docs.yml` to deploy the site to GitHub Pages.
- Branches are named `feat/*`, `fix/*` or `chore/*`, and pull requests are squash-merged.
