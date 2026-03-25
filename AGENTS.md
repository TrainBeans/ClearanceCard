# AGENTS.md

## Project Snapshot
- Stack: Spring Boot `4.0.4`, Java `17`, Maven Wrapper (`./mvnw`).
- Root package is `org.trainbeans.clearancecard`; component scanning starts there.
- Runtime dependencies: Web MVC, Thymeleaf, JDBC, H2 (in-memory), Actuator, DevTools, Docker Compose integration.
- **Jackson 3.x** (`tools.jackson.*`) is used — not `com.fasterxml.jackson.*`. ISO date serialization is the default; `WRITE_DATES_AS_TIMESTAMPS` no longer exists.

## Architecture and Data Flow
```
Browser UI  GET/POST /ui/clearance-cards  ─► UiController
JSON API    GET/POST /api/clearance-cards ─► ClearanceCardController
                                                      │
                                           ClearanceCardService
                                                      │
                                           ClearanceCardRepository (JdbcTemplate)
                                                      │
                         ┌────────────────────────────┴──────────────────────┐
                         clearance_card                  clearance_card_order_number
                         (one row per card)              (one row per order number)
```
- `ClearanceCardController` – `@Controller` (not `@RestController`): JSON methods carry `@ResponseBody`; `/print` and `/receipt` return Thymeleaf view names.
- `UiController` – `@Controller` at `/ui/clearance-cards`: GET list, GET new-card form, POST create → redirect.
- `WebConfig` – `WebMvcConfigurer` that redirects `/` → `/ui/clearance-cards`.
- `ClearanceCardRepository` – uses `SimpleJdbcInsert` for inserts; `fetchWithChildren()` avoids N+1 with a single `IN`-clause child query.
- `ClearanceCard` / `ClearanceCardRequest` – Java records in `model/`; each field maps to exactly one blank on NMRA/ORS Form 427-A.

## Package Layout
```
src/main/java/org/trainbeans/clearancecard/
  ClearanceCardApplication.java          ← @SpringBootApplication entry point
  config/    WebConfig.java              ← redirects / → /ui/clearance-cards
  controller/
    ClearanceCardController.java         ← JSON API + print/receipt HTML views
    UiController.java                    ← browser UI (list, form, POST create)
  exception/ ClearanceCardNotFoundException.java
  model/     ClearanceCard.java          ← Java record (response)
             ClearanceCardRequest.java   ← Java record (inbound DTO)
  repository/ClearanceCardRepository.java
  service/   ClearanceCardService.java

src/main/resources/
  application.properties                 ← app.railroad.name, datasource, H2 console
  schema.sql                             ← DDL; spring.sql.init.mode=always
  static/css/ui.css                      ← shared stylesheet for UI pages
  templates/
    clearance-card-print.html            ← full-page Form 427-A print view
    clearance-card-receipt.html          ← 58 mm thermal receipt (@page size: 58mm auto)
    ui/card-list.html                    ← card list + date-filter page
    ui/card-form.html                    ← new-card entry form (JS dynamic order rows)

src/test/java/org/trainbeans/clearancecard/
  ClearanceCardApplicationTests.java     ← @SpringBootTest smoke test
  controller/ClearanceCardControllerTest.java  ← @WebMvcTest, 7 tests
  repository/ClearanceCardRepositoryTest.java  ← @JdbcTest, 8 tests
```

## All Endpoints
| Method | Path | Type | Notes |
|--------|------|------|-------|
| `GET` | `/` | redirect | → `/ui/clearance-cards` |
| `GET` | `/ui/clearance-cards` | HTML | list; optional `?date=yyyy-MM-dd` |
| `GET` | `/ui/clearance-cards/new` | HTML | entry form |
| `POST` | `/ui/clearance-cards` | form | saves card, redirects to list |
| `POST` | `/api/clearance-cards` | JSON | creates card, returns 201 + Location |
| `GET` | `/api/clearance-cards` | JSON | all cards; optional `?date=` |
| `GET` | `/api/clearance-cards/{id}` | JSON | single card |
| `GET` | `/api/clearance-cards/{id}/print` | HTML | full-page Form 427-A |
| `GET` | `/api/clearance-cards/{id}/receipt` | HTML | 58 mm thermal receipt |
| `GET` | `/h2-console` | HTML | JDBC URL: `jdbc:h2:mem:clearancecard` |
| `GET` | `/actuator/health` | JSON | health check |

## Spring Boot 4 Test Package Names (differ from Boot 3)
| Annotation    | Import package                                          |
|---------------|---------------------------------------------------------|
| `@JdbcTest`   | `org.springframework.boot.jdbc.test.autoconfigure`      |
| `@WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure`    |
| `@MockitoBean`| `org.springframework.test.context.bean.override.mockito`|
| `ObjectMapper`| `tools.jackson.databind.ObjectMapper`                   |

`@WebMvcTest` scopes to `ClearanceCardController.class` only — `UiController` is not loaded in that slice.  
`@JdbcTest` requires `@Import(ClearanceCardRepository.class)`.  
Tests that inject `app.railroad.name` need `@TestPropertySource(properties = "app.railroad.name=Test Railroad")`.

## Critical Workflows
- **CI** (`.github/workflows/ci.yml`): runs `./mvnw clean test` on every push to `master` and every PR targeting `master`. Uses Java 17 (Temurin). Uploads Surefire reports as an artifact.
- **Release** (`.github/workflows/release.yml`): builds and pushes a Docker image to `ghcr.io/<owner>/<repo>` on every push to `master` (tags: `master`, `sha-<short>`) and on semver git tags `v*.*.*` (tags: `1.0.0`, `1.0`, `sha-<short>`). Uses `GITHUB_TOKEN` — no extra secrets needed.
- `Dockerfile`: two-stage build (eclipse-temurin:17-jdk-jammy → eclipse-temurin:17-jre-jammy).
- Run tests locally before pushing (16/16 must pass):
  ```bash
  cd /home/paul/Trainbeans/ClearanceCard
  ./mvnw clean test
  ```
- Run app locally:
  ```bash
  ./mvnw spring-boot:run
  ```
- Startup takes ~20 s; app listens on port 8080.

## Key application.properties Properties
| Property | Purpose |
|---|---|
| `app.railroad.name` | Railroad name shown in print headers and UI navbar |
| `spring.datasource.url` | Fixed H2 in-memory URL so H2 console can connect |
| `spring.sql.init.mode=always` | Runs `schema.sql` on every startup |

## Conventions
- Each form blank on NMRA/ORS Form 427-A maps to exactly one DB column; `order_count` stores the operator's stated count without validation against `orderNumbers.size()`.
- `clearance_card_order_number.id` is `GENERATED ALWAYS AS IDENTITY` — the `SimpleJdbcInsert` for that table uses `.usingColumns(...)` to avoid inserting the PK explicitly.
- Templates use `#temporals.format(card.issuedTime, 'h:mm a')` for time and `th:unless="${#lists.isEmpty(...)}"` / `th:unless="${#strings.isEmpty(...)}"` to suppress empty optional fields.
- Do **not** add `spring.jackson.serialization.write-dates-as-timestamps` — that enum constant was removed in Jackson 3.x and will crash the context.
- Add new endpoints under `org.trainbeans.clearancecard.*` and mirror tests under `src/test/java/...`.
