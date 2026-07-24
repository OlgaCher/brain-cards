# BrainCards

A neuropsychology-based web app that helps a parent run a short daily play session with their
child. Each day the app suggests five games — one from each brain-development zone — that suit the
child's age. The parent reads the instructions, plays, and ticks the game off; games played
recently aren't suggested again until a cooldown passes. If a game isn't working, an **AI coach**
can answer questions about it in the parent's own language.

Working proof of concept: authentication, the data model, the suggestion engine, a bilingual
server-rendered UI, a documented REST API, and live AI coaching are all functional. Scope is
deliberately limited to one child per parent.

## Tech stack

| Concern      | Choice                                                    |
|--------------|-----------------------------------------------------------|
| Language     | Java 21                                                   |
| Framework    | Spring Boot 3.5.16                                        |
| Build        | Gradle (Groovy DSL)                                       |
| Persistence  | Spring Data JPA / Hibernate, PostgreSQL                   |
| UI           | Thymeleaf — server-rendered, no JS framework, no build step |
| Auth         | Spring Security — form login (browser) + HTTP Basic (API)  |
| AI           | Google Gemini (`generateContent`), called via `RestClient` |
| API docs     | springdoc-openapi / Swagger UI                            |
| Validation   | Jakarta Bean Validation                                   |
| Boilerplate  | Lombok (entities), Java records (DTOs)                    |
| Tests        | JUnit 5, Mockito, AssertJ                                 |

## Package structure

```
com.braincards
├── ai           AiCoachService interface + Gemini implementation
│   └── gemini   GeminiClient (HTTP) + request/response records
├── config       SecurityConfig, LocaleConfig, OpenApiConfig
├── model        JPA entities + the Outcome enum
├── repository   Spring Data JPA repositories
├── service      Business logic, ownership enforcement, the suggestion engine
├── controller   Thymeleaf page controllers + REST controllers + the error handler
└── dto          Request/response records — entities are never exposed directly
```

## Data model

- **Parent** — one row per registered user; `email` is the login username, `passwordHash` is
  BCrypt-hashed, never plaintext.
- **Child** — one-to-one with `Parent` (kept as its own table/entity, not flattened into
  `Parent`, so multi-child support is a schema-compatible future change).
- **Zone** / **ZoneTranslation** — a brain-development category (`regulation`, `spatial`, ...)
  and its localized display name.
- **Game** / **GameTranslation** — belongs to a `Zone`, has an age range
  (`minAgeMonths`/`maxAgeMonths`), an `active` flag, and an optional per-game `cooldownDays`
  override; its localized title/instructions live in `GameTranslation`.
- **SessionLog** — one row per game actually **played**: which child, which game, when, the
  `Outcome` (`EASY` / `JUST_RIGHT` / `TOO_HARD` / `REFUSED`), duration, optional note.
- **SwapLog** — one row per game **swapped away** on a given day. Deliberately *not* a
  `SessionLog`: a swap is not a play, so it must never feed the cooldown — it only hides that game
  for the rest of the day.
- **DailyPick** — one row per zone per day when the parent explicitly picks a game
  ("Choose for today"), overriding the automatic suggestion.

There is no "done"/"hidden" flag on `Game` — whether a game is suggestible today is *computed*
from these history tables at request time.

### Why translations live in their own tables

Translatable content is **not** stored as `name_uk`/`name_en` columns. `Zone` and `Game` each have
a companion `*Translation` table with a `(entity_id, locale)` unique constraint, so adding a
language means inserting **rows**, not altering the schema.

`spring.jpa.hibernate.ddl-auto=update` creates/updates tables from the entities on startup —
no Flyway/Liquibase by design (project scope).

## The suggestion engine

Each day the parent sees **one game per zone**. For a given zone, the choice is made in this
order of precedence:

1. **An explicit pin** (`DailyPick`, via "Choose for today") wins outright — shown even if the
   game is on cooldown or was swapped, because the parent deliberately asked for it.
2. Otherwise an **auto-pick** from the eligible candidates.

A zone is **dropped entirely** for the day once any of its games is marked done — that slot is
used up. A game **swapped away** today is excluded from the auto-pick pool but returns tomorrow.

A game is an eligible candidate when **all** of:
- `game.active = true`
- the child's age in months falls within `[minAgeMonths, maxAgeMonths]`
- the child has **not** played it within the cooldown window — `game.cooldownDays` if set,
  otherwise `braincards.suggestion.cooldown-days` (default `7`)

This is a single native SQL query with a `NOT EXISTS` subquery against `session_log`
([`GameRepository.findCandidates`](src/main/java/com/braincards/repository/GameRepository.java)) —
games are never loaded into memory and filtered in Java.

**The auto-pick is "stable random."** A truly random pick would reshuffle the cards on every page
refresh and look broken. Instead the index is derived from `(childId, zoneId, today)`, so the
selection looks arbitrary, varies day to day, changes after a swap (the candidate set shrinks) —
but stays identical across refreshes within a day. See `DailySuggestionService.pickForZone`.

## Ownership rule

A parent may only ever read or modify **their own** child and session logs — enforced in the
service layer, not just the URL shape:
- `/child` never takes an id in the URL at all; it's always resolved from the authenticated
  parent's own id (`ChildService.findChildEntity`), so there's no id to spoof.
- `/session-log/{id}` *does* take a client-supplied id; `SessionLogService` explicitly checks
  the log's owning parent against the caller and throws `AccessDeniedException` (→ HTTP 403).

## Running locally

PostgreSQL must be running with a `braincards_dev` database. Secrets come from environment
variables — never hardcoded or committed.

```powershell
$env:DB_PASSWORD = "your-postgres-password"
$env:GEMINI_API_KEY = "your-gemini-key"   # optional; without it only the AI coach is unavailable
.\gradlew.bat bootRun
```

The app listens on `http://localhost:8080/braincards/v1`.

> **Note on the database port:** `application.properties` points at
> `jdbc:postgresql://localhost:4000/braincards_dev` because the local PostgreSQL instance used for
> development runs on port **4000**, not the 5432 default. Change it to match your own setup.

### Seeding content

The app ships no content by default. [sql/seed_content.sql](sql/seed_content.sql) inserts 5 zones
and 30 "no materials needed" games with UA + EN translations. Run it **after** the app has booted
once (so Hibernate has created the tables):

```powershell
$env:PGCLIENTENCODING = "UTF8"
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -h localhost -p 4000 -U postgres -d braincards_dev -f sql\seed_content.sql
```

`PGCLIENTENCODING=UTF8` matters on Windows — without it psql misreads the Cyrillic text. See the
file's header comment for how to re-seed from scratch.

### Running the tests

```powershell
.\gradlew.bat test
```

## Base URL — why `/braincards/v1`

`server.servlet.context-path=/braincards/v1` prefixes **every** route — pages and API alike — so
nothing needs an extra `/api` segment:

- `http://localhost:8080/braincards/v1/login`
- `http://localhost:8080/braincards/v1/home`
- `http://localhost:8080/braincards/v1/child`
- `http://localhost:8080/braincards/v1/swagger-ui/index.html`

## The web UI

Server-rendered Thymeleaf, hand-written CSS, and essentially **zero JavaScript** — even the card
flip is pure CSS (a hidden checkbox driving a 3D transform).

| Route | What it does |
|---|---|
| `/login`, `/register` | Auth pages |
| `/home` | Greeting, child summary, today's five cards, all zones |
| `/home/zones/{zoneId}` | Browse a zone's games, "Choose for today" |
| `/home/games/{gameId}/coach` | Ask the AI coach about a game |

Card actions (`POST`): `/home/games/{id}/complete`, `/swap`, `/choose`.

Each zone has a fixed accent colour derived from its id, so the same zone is the same colour on
every screen.

## Authentication: two mechanisms, one user store

Both read from the same `Parent` table via `ParentUserDetailsService` / `BCryptPasswordEncoder`
— same credentials, two transports:

| | Browser pages | REST API |
|---|---|---|
| Mechanism | Session-based form login | HTTP Basic |
| Session | Created and reused (cookie) | `STATELESS` — never read or created |
| CSRF | Enabled (cookie-backed token) | Disabled — safe *because* the chain is stateless: with no session cookie in play there's no ambient credential for a malicious page to ride |
| Unauthenticated | 302 redirect to `/login` | 401 with `WWW-Authenticate: Basic` |

**Postman**: Authorization tab → *Basic Auth*, on every request. Being logged into the browser does
**not** authenticate API calls — the API chain is stateless by design.

**Swagger UI** (`/braincards/v1/swagger-ui/index.html`): click **Authorize** once, then "Try it out".

> `SecurityConfig.API_PATHS` lists the REST base paths. **Add every new REST controller's path
> there** — otherwise it falls through to the web chain and returns a login redirect instead of a 401.

## REST API reference

All paths relative to `http://localhost:8080/braincards/v1`. All require Basic auth.

| Method | Path | Notes |
|---|---|---|
| GET | `/parent/me` | current parent's profile |
| GET / POST / PUT / DELETE | `/child` | my child (one per parent) |
| GET | `/zone`, `/zone/{id}` | zones with localized names |
| POST | `/zone` | create a zone (with translations) |
| GET | `/game?zoneId=`, `/game/{id}` | games, optional zone filter |
| POST / PUT / DELETE | `/game`, `/game/{id}` | manage games |
| GET | `/session-log?date=` | my session logs, optional date filter |
| POST / PUT / DELETE | `/session-log`, `/session-log/{id}` | manage my logs (ownership enforced) |
| GET | `/suggestion/today` | today's suggested games for my child |
| POST | `/coach` | ask the AI coach — `{"gameId":1,"question":"..."}` |

Errors use a consistent shape via `ApiExceptionHandler`: `{"status": 404, "message": "..."}` —
400 (validation), 403 (ownership), 404 (missing), 503 (AI unavailable).

### Examples

```bash
# today's suggestions
curl -u you@example.com:yourpassword http://localhost:8080/braincards/v1/suggestion/today

# ask the AI coach
curl -u you@example.com:yourpassword -X POST http://localhost:8080/braincards/v1/coach \
  -H "Content-Type: application/json" \
  -d '{"gameId":1,"question":"My child loses interest after a minute - any tips?"}'
```

## AI coach

A parent stuck on a game ("she won't sit still for this") gets a short, practical, age-aware
answer in their current UI language.

**Design:** [`AiCoachService`](src/main/java/com/braincards/ai/AiCoachService.java) is the
interface; `GeminiAiCoachService` builds the prompt (game title, instructions, child's age in
months, the question, target language) from a single `COACH_PROMPT_TEMPLATE` constant;
`GeminiClient` owns the HTTP call. **The provider is isolated in that one class** — swapping to a
different LLM is a one-class change, and the prompt logic stays unit-testable with a mocked client.

Failures raise `AiCoachException` → **HTTP 503** on the API, and render inline on the coach page
rather than crashing it. The coach page uses post-redirect-get so refreshing doesn't re-send the
question (and re-bill the API).

**Configuration** (`application.properties`):

```properties
braincards.ai.gemini.base-url=https://generativelanguage.googleapis.com/v1beta
braincards.ai.gemini.model=gemini-3.1-flash-lite
braincards.ai.gemini.api-key=${GEMINI_API_KEY:}
```

The key is sent in the `x-goog-api-key` **header**, not the query string, so it never lands in
request URLs or access logs. The app boots fine without a key — the coach endpoint simply returns
503 "not configured".

> **Cost warning — Gemini's free tier is not universally available.** In this project's region the
> free tier returned `429 RESOURCE_EXHAUSTED` with `limit: 0` (no free allocation at all, rather
> than a temporary rate limit), and billing had to be enabled. `flash-lite` is among the cheapest
> models, so real usage costs are negligible — but it **is** a paid API, and the key can spend
> money. Keep it in an environment variable.

## Internationalization

Two independent mechanisms — don't confuse them:

- **UI text** (labels, page titles) — Spring resource bundles (`messages_en.properties`,
  `messages_ua.properties`), a session `LocaleResolver`, and a `?lang=` param. Every template uses
  `#{...}` keys; nothing is hardcoded.
- **Content text** (zone names, game titles/instructions) — the `*Translation` DB tables, resolved
  by the request locale, falling back to `en`.

**A quirk worth knowing:** the app uses `"UA"` (not ISO `"uk"`) as the Ukrainian locale code in
URLs and `Parent.locale`. But `java.util.Locale` always lowercases the language subtag —
`new Locale("UA").toString()` is `"ua"` — so the bundle file must be `messages_ua.properties`
(lowercase), and translation lookups compare locales case-insensitively to bridge the two.
Documented inline in `LocaleConfig` and `GameService`/`ZoneService`.

> **Gotcha that cost real debugging time:** springdoc (Swagger) registers its own `MessageSource`
> bean, which made Spring's auto-configured one back off and silently fall back to an empty
> `DelegatingMessageSource` — every translated string on the site broke at once. Fixed by defining
> the `messageSource` bean explicitly in `LocaleConfig`. If *all* i18n breaks simultaneously,
> suspect bean wiring, not the properties files.

## What's not built yet

- **Outcome picker in the UI** — "Done" always records `JUST_RIGHT`. The `Outcome` enum
  (`EASY` / `JUST_RIGHT` / `TOO_HARD` / `REFUSED`) and the API already support the full range; the
  UI just doesn't ask yet.
- **Outcome-aware cooldowns** — a game marked `EASY` could return sooner than one marked
  `TOO_HARD`. See the TODO in `DailySuggestionService`; today every game uses one fixed window.
- **Progress history for parents** — `SessionLog` captures the data, nothing surfaces it yet.
- **Multi-child support** — `Child` is already its own entity/table for this reason, but the app
  assumes exactly one child per parent.
- **More than 5 zone colours** — accents are `zoneId % 5`, so a 6th zone reuses a colour.
