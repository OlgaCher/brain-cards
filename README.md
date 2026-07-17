# BrainCards

A neuropsychology-based web app that helps a parent run a short daily play session with their
child. Each day the app suggests a handful of games drawn from different "brain zones"
(memory, attention, motor skills, etc.). A parent logs what was actually played and how it
went; games that were played recently are not suggested again until a cooldown window passes.

This is a **learning project skeleton**: the architecture, auth, data model, and REST API are
fully wired and working, but the app is intentionally scoped to a single child per parent.

## Tech stack

| Concern         | Choice                                              |
|------------------|------------------------------------------------------|
| Language         | Java 21                                               |
| Framework        | Spring Boot 3.5.16                                    |
| Build            | Gradle (Groovy DSL)                                   |
| Persistence      | Spring Data JPA / Hibernate, PostgreSQL               |
| Frontend         | Thymeleaf (server-rendered, no JS framework)          |
| Auth             | Spring Security — form login (browser) + HTTP Basic (API) |
| API docs         | springdoc-openapi / Swagger UI                        |
| Validation       | Jakarta Bean Validation                               |
| Boilerplate      | Lombok (entities), Java records (DTOs)                |

## Why the database looks the way it does

Translatable content (zone names, game titles/instructions) is **not** stored as `name_uk`/
`name_en` columns. Instead, `Zone`/`Game` each have a companion `*Translation` table
(`zone_translation`, `game_translation`) with a `(entity_id, locale)` unique constraint — so
adding a new UI language is a matter of inserting new rows, not altering the schema.

`Hibernate.hbm2ddl.auto=update` creates/updates tables automatically from the JPA entities on
startup — there's no Flyway/Liquibase migration history by design (per project scope).

## Package structure

```
com.braincards
├── config       SecurityConfig, LocaleConfig, OpenApiConfig
├── model        JPA entities + the Outcome enum
├── repository   Spring Data JPA repositories
├── service      Business logic, ownership enforcement, the cooldown query
├── controller   Thymeleaf page controllers + REST controllers + the error handler
└── dto          Request/response records — entities are never exposed directly
```

## Data model

- **Parent** — one row per registered user; `email` is the login username, `passwordHash` is
  BCrypt-hashed, never plaintext.
- **Child** — one-to-one with `Parent` (kept as its own table/entity, not flattened into
  `Parent`, so multi-child support is a schema-compatible future change).
- **Zone** / **ZoneTranslation** — a brain-development category ("memory", "attention", ...)
  and its localized display name.
- **Game** / **GameTranslation** — belongs to a `Zone`, has an age range
  (`minAgeMonths`/`maxAgeMonths`), an `active` flag, and an optional per-game `cooldownDays`
  override; its localized title/instructions live in `GameTranslation`.
- **SessionLog** — one row per game actually played: which child, which game, when, the
  `Outcome` (`EASY` / `JUST_RIGHT` / `TOO_HARD` / `REFUSED`), duration, and an optional note.
  Indexed on `(child_id, played_on)` and `(child_id, game_id)`.

There is no "hidden"/"done" flag on `Game` — whether a game is suggestible today is *computed*
from `SessionLog` at request time (see below).

## The suggestion / cooldown rule

A game is suggested to a child today if **all** of:
- `game.active = true`
- the child's current age (in months) falls within `[minAgeMonths, maxAgeMonths]`
- the child has **not** played it within the cooldown window — `game.cooldownDays` if set,
  otherwise the app-wide default `braincards.suggestion.cooldown-days` (application.properties,
  default `7`)

This is a single native SQL query with a `NOT EXISTS` subquery against `session_log`
(`GameRepository.findCandidates`) — games are never loaded into memory and filtered in Java.
See [`DailySuggestionService`](src/main/java/com/braincards/service/DailySuggestionService.java)
for the TODO on a planned future refinement: outcome-aware cooldowns (e.g. a game marked `EASY`
becoming suggestible again sooner than one marked `TOO_HARD`).

## Ownership rule

A parent may only ever read or modify **their own** child and session logs — enforced in the
service layer, not just the URL shape:
- `/child` never takes an id in the URL at all; it's always resolved from the authenticated
  parent's own id (`ChildService.findChildEntity`), so there's no id to spoof.
- `/session-log/{id}` *does* take a client-supplied id; `SessionLogService` explicitly checks
  the log's owning parent against the caller and throws Spring Security's `AccessDeniedException`
  (→ HTTP 403) on mismatch.

## Running locally

PostgreSQL must already be running with a `braincards_dev` database. Password is read from an
environment variable — it is never hardcoded or committed.

```powershell
$env:DB_PASSWORD = "your-postgres-password"
.\gradlew.bat bootRun
```

The app listens on `http://localhost:8080/braincards/v1` (see
[Base URL](#base-url--why-braincardsv1) below).

## Base URL — why `/braincards/v1`

`server.servlet.context-path=/braincards/v1` prefixes **every** route in the app — pages and
API alike — so nothing needs an explicit `/api` segment on top of it:

- `http://localhost:8080/braincards/v1/login`
- `http://localhost:8080/braincards/v1/home`
- `http://localhost:8080/braincards/v1/child`
- `http://localhost:8080/braincards/v1/swagger-ui/index.html`

## Authentication: two mechanisms, one user store

Both read from the same `Parent` table via `ParentUserDetailsService` / `BCryptPasswordEncoder`
— it's the same credentials either way, just two different transports:

| | Browser pages (`/login`, `/register`, `/home`, ...) | REST API (`/child`, `/game`, `/session-log`, ...) |
|---|---|---|
| Mechanism | Session-based form login | HTTP Basic |
| Session | Created and reused (cookie) | `SessionCreationPolicy.STATELESS` — no session read or created, ever |
| CSRF | Enabled (cookie-backed token) | Disabled — safe *because* the chain is stateless: without a session cookie in play, there's no ambient credential for a malicious page to ride, which is the attack CSRF protects against |
| Unauthenticated request | 302 redirect to `/login` | 401 with a `WWW-Authenticate: Basic` header |

**Testing in Postman**: set the Authorization tab to *Basic Auth*, enter a registered parent's
email/password on every request. No CSRF token needed for these endpoints. (Note: since the API
chain is stateless, being logged into the browser pages in the same tab does *not* also
authenticate API calls — Basic Auth is required every time.)

**Testing in Swagger UI** (`/braincards/v1/swagger-ui/index.html`): click **Authorize**, enter
the same email/password once — it's applied to every "Try it out" call automatically.

## REST API reference

All paths below are relative to `http://localhost:8080/braincards/v1`.

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/parent/me` | Basic | current parent's profile |
| GET | `/child` | Basic | my child |
| POST | `/child` | Basic | create my child (one per parent) |
| PUT | `/child` | Basic | update my child |
| DELETE | `/child` | Basic | delete my child |
| GET | `/zone` | Basic | list zones (localized name) |
| GET | `/zone/{id}` | Basic | one zone |
| POST | `/zone` | Basic | create a zone (with translations) |
| GET | `/game?zoneId=` | Basic | list games, optional zone filter |
| GET | `/game/{id}` | Basic | one game |
| POST | `/game` | Basic | create a game (with translations) |
| PUT | `/game/{id}` | Basic | update a game |
| DELETE | `/game/{id}` | Basic | delete a game |
| GET | `/session-log?date=` | Basic | my session logs, optional date filter |
| POST | `/session-log` | Basic | log a played game |
| PUT | `/session-log/{id}` | Basic | update a session log (must be mine) |
| DELETE | `/session-log/{id}` | Basic | delete a session log (must be mine) |
| GET | `/suggestion/today` | Basic | today's candidate games for my child |

Errors are a consistent JSON shape via `ApiExceptionHandler`: `{"status": 404, "message": "..."}`.

### Example: seed a zone, then a game

```bash
curl -u you@example.com:yourpassword -X POST http://localhost:8080/braincards/v1/zone \
  -H "Content-Type: application/json" \
  -d '{"code":"memory","translations":[{"locale":"en","name":"Memory"},{"locale":"UA","name":"Пам'\''ять"}]}'

curl -u you@example.com:yourpassword -X POST http://localhost:8080/braincards/v1/game \
  -H "Content-Type: application/json" \
  -d '{"zoneId":1,"minAgeMonths":24,"maxAgeMonths":48,"cooldownDays":3,
       "translations":[{"locale":"en","title":"Memory Cards","instructions":"Match the pairs."}]}'

curl -u you@example.com:yourpassword http://localhost:8080/braincards/v1/suggestion/today
```

## Internationalization

Two independent mechanisms, don't confuse them:

- **UI text** (button labels, page titles) — Spring i18n resource bundles
  (`messages_en.properties`, `messages_ua.properties`), resolved via a session-based
  `LocaleResolver` + `?lang=` query param. All Thymeleaf templates use `#{...}` keys, nothing
  hardcoded.
- **Content text** (zone names, game titles) — the `*Translation` DB tables, resolved by the
  request's current locale with fallback to `en` if the requested locale's translation is
  missing.

**A quirk worth knowing**: the app uses `"UA"` (not the ISO `"uk"`) as the Ukrainian locale
code in URLs and the stored `Parent.locale` value, per project preference. But `java.util.Locale`
always lowercases the language subtag internally — `new Locale("UA").toString()` is `"ua"` — so
the physical bundle file is unavoidably `messages_ua.properties` (lowercase), and translation
lookups compare locale strings case-insensitively to bridge the two. This is documented inline
in `LocaleConfig` and `GameService`/`ZoneService`.

## What's not built yet

- **`AiCoachService`** — the real-time AI coaching placeholder interface described in the
  original spec is deferred to a future iteration (to be built with an agentic approach).
- **Content seeding** — no automated seeding is wired into the app (per project scope, no
  Flyway/Liquibase). [sql/seed_content.sql](sql/seed_content.sql) is a one-off script with
  5 zones and 30 no-materials games (UA + EN) to run manually against `braincards_dev` after
  the app has booted once and created the tables — see the comment header in that file for
  how to run it and re-seed from scratch. The `POST /zone` and `POST /game` endpoints above
  still work for adding content one at a time if you'd rather do that.
- **Outcome-aware cooldowns** — see the TODO in `DailySuggestionService`.
- **Multi-child support** — `Child` is already its own entity/table for this reason, but the
  app currently assumes exactly one child per parent.
