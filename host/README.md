# host

The application. One process, every module.

## What lives here

- `Application` — the module list, imported by name. This file is the complete answer to
  "what is running?", and removing a module you do not need is one line here plus one
  dependency in `pom.xml`.
- `config/JpaConfiguration` — entity and repository discovery, kept off the application
  class so web slice tests do not drag JPA in.
- `config/OpenApiConfiguration` — document title and the bearer scheme, so Swagger UI's
  Authorize button works.
- `web/HealthController` — `GET /api/v1/health`.
- `application.yml` — the whole configuration surface, every secret from the environment.
- The tests that only make sense with every module present: `ArchitectureRulesTest` and
  `ApplicationIT`.

## Why the host owns almost no code

Endpoints live in the modules that implement them, not here. Delete a module and its
endpoints go with it; the host does not slowly accumulate knowledge of every feature.

## Tests

`ArchitectureRulesTest` (ArchUnit) enforces what Maven cannot see — that no module reaches
into another module's `internal` package, that `core` depends on nothing, that the platform
never depends on `domain`, and that only `billing` knows RevenueCat exists.

`ApplicationIT` (Testcontainers) starts real Postgres and the whole application. Its main
value is proving every module's migrations apply to one database in one order and that
`ddl-auto: validate` agrees with the result — an entity field with no column otherwise
surfaces on deploy. It **skips when Docker is not running**, so `./mvnw verify` stays green
on a machine without it; CI has Docker and runs it for real.

## Adding a module to the application

1. Add the dependency to `host/pom.xml`.
2. Add its configuration class to `@Import` in `Application`.
3. Add its migration location to `spring.flyway.locations` in `application.yml`.

Those three places are the only ones that know a module exists.
