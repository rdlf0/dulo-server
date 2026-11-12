# Contributing

## Workflow

1. Open or pick an issue.
2. Create a branch from `main`:
   - `feat/<short-name>` for features
   - `fix/<short-name>` for bug fixes
   - `chore/<short-name>` for build, CI and dependency changes
3. Open a pull request.
4. Pull requests are squash-merged once checks pass and the PR is approved.

## Versioning and releases

Dulo follows [semantic versioning](https://semver.org). Releases are started by hand, so one
release can bundle several merged pull requests:

1. In the Actions tab, open **Release** → **Run workflow** on `main` and pick the version type:
   - `major`: breaking changes
   - `minor`: new features
   - `patch`: bug fixes and internal changes only
2. `maven-release-plugin` (`release:prepare`) sets the version using the matching policy
   (`SemVerMajorRelease`, `SemVerMinorRelease` or `SemVerPatchDevelopment`) and runs the full build
   with all tests. If it passes, it commits the release version, tags it `vX.Y.Z`, commits the
   next `-SNAPSHOT` version and pushes. These commits are marked `[skip ci]`. If the build fails,
   nothing is committed or pushed.
3. A GitHub release is created with the JAR, the OpenAPI document and the config JSON Schema
   attached. The JAR gets a build provenance attestation.
4. The **Docs** workflow publishes the docs site of the release to GitHub Pages.

CI runs on pull requests only. `main` changes only through pull requests, and a release runs the
full build again before it publishes anything.

### Distribution

Dulo ships as one image with the server and the web client, built by the web client project. 
That build downloads the server JAR from a GitHub release and checks where it came from:

```sh
gh release download vX.Y.Z --repo rdlf0/dulo-server --pattern 'dulo-server-*.jar'
gh attestation verify dulo-server-X.Y.Z.jar --repo rdlf0/dulo-server
```

The JAR runs with `java -jar` on Java 25. It needs no extra JVM options.

### Repository setup

Release commits and tags are pushed by a GitHub App, so they show up as `<app-name>[bot]`
and not as a person. The workflow gets a token from the app that expires after one hour.

1. **Create the app:** [New GitHub App](https://github.com/settings/apps/new):
   - **Name:** e.g. `dulo-release`.
   - **Homepage URL:** the repository URL.
   - **Webhook:** uncheck **Active**.
   - **Repository permissions:** **Contents: Read and write** (Metadata: Read-only is added
     automatically). Nothing else.
   - **Where can this GitHub App be installed?** **Only on this account**.
2. **Generate a private key** on the app's page (**Private keys** → **Generate a private key**),
   and note the **Client ID** at the top of the page.
3. **Install the app** (**Install App**) on this repository only.
4. **Repository secrets and variables** (**Settings** → **Secrets and variables** → **Actions**):
   - Variable `RELEASE_APP_CLIENT_ID`: the app's Client ID.
   - Secret `RELEASE_APP_PRIVATE_KEY`: the full contents of the downloaded `.pem` file.
5. **Ruleset on `main`** (**Settings** → **Rules** → **Rulesets**):
   - Require a pull request, require the `CI` and `Docs` checks, block force
     pushes and deletion.
   - Turn on **Require branches to be up to date before merging**, so the code that lands on
     `main` is the code CI tested.
   - **Bypass list:** add the app (**Add bypass** → **Apps**) with **Always allow**, so it can
     push the release commits.

### Docs site

The docs site is the Maven site of the project: Swagger UI with the OpenAPI document, the Javadoc
of all modules, test and coverage reports, and project information. The **Docs** workflow deploys it to GitHub
Pages on every release. No branch is involved.

1. Turn on Pages: **Settings** → **Pages** → **Build and deployment** → **Source:** GitHub
   Actions.
2. Every release publishes the site. To publish or republish it in between: **Actions** →
   **Docs** → **Run workflow** from `main`, with a tag or `main` as the ref. The `github-pages`
   environment only accepts deployments from `main`, so always start it from `main`.

Build the site locally, then open `target/staging/index.html`:

```sh
./mvnw verify site site:stage
```

The site's menu is in `src/site/site.xml`, and the Swagger UI page in `src/site/resources/api/`.
The build adds the Swagger UI files and `api/openapi.json` next to it.

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

## OpenAPI document

`api/openapi.json` describes the REST API. Clients generate their code from it, and the docs site
shows it. A test fails when it is out of date. Regenerate it with:

```sh
./mvnw -pl dulo-service -am test -Dtest=OpenApiDocumentTest -Dsurefire.failIfNoSpecifiedTests=false -Ddulo.openapi.update=true
```

Describe each endpoint with the Swagger annotations (`@Tag`, `@Operation`, `@ApiResponse`).
Response record components are required in the document unless they are `@Nullable`.
