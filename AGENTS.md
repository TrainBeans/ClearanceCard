# AGENTS.md

## Project Snapshot
- Stack: Spring Boot `4.0.4`, Java `17`, Maven Wrapper (`./mvnw`).
- Root package is `org.trainbeans.clearancecard`; component scanning starts there.
- Runtime dependencies: Web MVC, Thymeleaf, JDBC, H2 (in-memory), Actuator, DevTools, Docker Compose integration.
- **Jackson 3.x** (`tools.jackson.*`) is used — not `com.fasterxml.jackson.*`. ISO date serialization is the default; `WRITE_DATES_AS_TIMESTAMPS` no longer exists.

## Architecture and Data Flow
```
POST /api/clearance-cards          ─► ClearanceCardController
GET  /api/clearance-cards?date=    ─► ClearanceCardService
GET  /api/clearance-cards/{id}                 │
GET  /api/clearance-cards/{id}/print           ▼
                                   ClearanceCardRepository  (JdbcTemplate)
                                               │
                              ┌────────────────┴──────────────────────┐
                              clearance_card              clearance_card_order_number
                              (one row per card)          (one row per order number)
```
- `ClearanceCardController` – `@Controller` (not `@RestController`): JSON methods carry `@ResponseBody`; the `/print` endpoint returns a Thymeleaf view name.
- `ClearanceCardRepository` – uses `SimpleJdbcInsert` for inserts; `fetchWithChildren()` avoids N+1 with a single `IN`-clause child query.
- `ClearanceCard` / `ClearanceCardRequest` – Java records in `model/`; each field maps to exactly one blank on NMRA/ORS Form 427-A.

## Package Layout
```
src/main/java/org/trainbeans/clearancecard/
  controller/  ClearanceCardController.java
  exception/   ClearanceCardNotFoundException.java
  model/        ClearanceCard.java, ClearanceCardRequest.java
  repository/  ClearanceCardRepository.java
  service/     ClearanceCardService.java
src/main/resources/
  schema.sql                          ← DDL run at startup
  templates/clearance-card-print.html ← Thymeleaf Form 427-A layout
  application.properties
src/test/java/org/trainbeans/clearancecard/
  ClearanceCardApplicationTests.java  ← full-context smoke test
  controller/  ClearanceCardControllerTest.java  (@WebMvcTest)
  repository/  ClearanceCardRepositoryTest.java  (@JdbcTest)
```

## Spring Boot 4 Test Package Names (differ from Boot 3)
| Annotation    | Import package                                          |
|---------------|---------------------------------------------------------|
| `@JdbcTest`   | `org.springframework.boot.jdbc.test.autoconfigure`      |
| `@WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure`    |
| `@MockitoBean`| `org.springframework.test.context.bean.override.mockito`|
| `ObjectMapper`| `tools.jackson.databind.ObjectMapper`                   |

## Critical Workflows
- Run tests (verified, 16/16 pass):
  ```bash
  cd /home/paul/Trainbeans/ClearanceCard
  ./mvnw -q test
  ```
- Run app locally (Docker Compose disabled):
  ```bash
  SPRING_DOCKER_COMPOSE_ENABLED=false ./mvnw -q spring-boot:run
  ```
- Useful URLs after startup:
  - `http://localhost:8080/api/clearance-cards`
  - `http://localhost:8080/api/clearance-cards/{id}/print`
  - `http://localhost:8080/actuator`
  - `http://localhost:8080/h2-console`  (JDBC URL: `jdbc:h2:mem:clearancecard`)

## Key application.properties Properties
| Property | Purpose |
|---|---|
| `app.railroad.name` | Railroad name shown in the Form 427-A print header |
| `spring.datasource.url` | Fixed H2 in-memory URL so H2 console can connect |
| `spring.sql.init.mode=always` | Runs `schema.sql` on every startup |

## Conventions
- Each form blank on NMRA/ORS Form 427-A maps to exactly one DB column; `order_count` stores the operator's stated count without validation.
- `clearance_card_order_number.id` is `GENERATED ALWAYS AS IDENTITY` — the `SimpleJdbcInsert` for that table uses `.usingColumns(...)` to avoid inserting the PK explicitly.
- The print Thymeleaf template uses `#temporals.format(card.issuedTime, 'h:mm a')` for AM/PM formatting.
- Do **not** add `spring.jackson.serialization.write-dates-as-timestamps` — that enum constant was removed in Jackson 3.x and will crash the context.
- Add new endpoints under `org.trainbeans.clearancecard.*` and mirror tests under `src/test/java/...`.
