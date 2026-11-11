# Contributing

## Workflow

1. Open or pick an issue.
2. Create a branch from `main`:
   - `feat/<short-name>` for features
   - `fix/<short-name>` for bug fixes
   - `chore/<short-name>` for build, CI and dependency changes
3. Open a pull request and add **exactly one** release label:
   - `release: major`: breaking change
   - `release: minor`: new feature
   - `release: patch`: bug fix or internal change
4. Pull requests are squash-merged once checks pass and the PR is approved.

## Versioning and releases

Dulo follows [semantic versioning](https://semver.org). Every merge to `main` produces a release:

1. The **Release** workflow reads the merged PR's label.
2. `maven-release-plugin` sets the version using the matching policy (`SemVerMajorRelease`,
   `SemVerMinorRelease` or `SemVerPatchDevelopment`), creates a signed `vX.Y.Z` tag and commits the
   next `-SNAPSHOT` version. These commits are marked `[skip ci]`.
3. The Docker image is pushed to `ghcr.io/rdlf0/dulo-server` for amd64 and arm64, tagged
   `X.Y.Z`, `X.Y`, `X` and `latest`.
4. A GitHub release is created with the JAR and the config JSON Schema attached.

A release can also be started by hand from the Actions tab (**Release** → **Run workflow**).

### Repository setup

- **Branch protection on `main`:** require a pull request, require the `CI`, `Docker image` and
  `Validate Labels` checks, and block direct pushes.
- **Secrets used by the Release workflow:**
  - `RELEASE_TOKEN`: a GitHub App token or personal access token that may push to the protected
    `main` branch (it pushes the version commit and tag).
  - `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE` (optional): key that signs release commits and tags. The
    git author name and email come from the key.

## Code style

- Java code uses Google Java Format with the AOSP style. The build fails on unformatted code; run
  `./mvnw com.spotify.fmt:fmt-maven-plugin:format` to fix it.
- The shared IntelliJ settings in `.idea/` (code style, inspections, required plugins) define the
  rest. In short:
  - Class members go in this order: static fields, instance fields, constructors, static
    methods, instance methods, nested types. Fields are ordered public, protected, package-private,
    then private.
  - Every parameter and local variable is `final`, including catch, for-each and pattern variables.
  - Instance method calls are qualified with `this.`.
  - Utility classes are `final` and have a private constructor.
  - The last enum constant ends with a comma and the `;` goes on its own line, so adding a constant
    changes one line.
- Every package has a `package-info.java` with JSpecify's `@NullMarked`. Mark values that may be
  null with `@Nullable`.
- Keep lists alphabetically sorted. In POMs, dependencies are grouped into internal, 3rd party and
  test sections, and sorted by groupId, then artifactId, within each section.
- In POMs, versions, configuration and executions go in `dependencyManagement` and
  `pluginManagement`. `dependencies` and `plugins` only name the artifact. Keep a blank line
  between version properties, so a Dependabot update changes one line.
- Declare every dependency the code uses directly. `maven-dependency-plugin` fails the build on
  used but undeclared and on declared but unused dependencies.
- Use American English.
- Keep the architecture rules green (`ArchitectureTest`): `dulo-domain` and `dulo-core` must
  not depend on frameworks, and adapters must not depend on each other.

## Test coverage

`./mvnw verify` writes a coverage report per module and one for all modules together, including
the integration tests, to `dulo-service/target/site/jacoco-aggregate/`. CI fails when line coverage
drops below 85% or branch coverage below 70%.

## Configuration schema

`schema/dulo-config.schema.json` is generated from `DuloProperties`. A test fails when it is out
of date. Regenerate it with:

```sh
./mvnw -pl dulo-service -am test -Dtest=ConfigSchemaTest -Dsurefire.failIfNoSpecifiedTests=false -Ddulo.schema.update=true
```
