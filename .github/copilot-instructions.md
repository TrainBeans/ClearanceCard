# Copilot Instructions — ClearanceCard

## What This Repository Does
A Spring Boot web application for railroad dispatchers to enter, store, and print **NMRA/ORS Form 427-A clearance cards** used in timetable-and-train-order operations. It provides a browser UI (Thymeleaf) and a JSON REST API, with two print layouts: a full-page Form 427-A and a 58 mm thermal-receipt format.

---

## Stack
| Component | Version |
|-----------|---------|
| Java (OpenJDK) | 17.0.15 |
| Spring Boot | 4.0.4 |
| Maven Wrapper | 3.9.14 (`./mvnw`) |
| Jackson | **3.x** — package `tools.jackson.*` (NOT `com.fasterxml.jackson.*`) |
| Database | H2 in-memory; schema auto-loaded from `src/main/resources/schema.sql` |
| Templates | Thymeleaf 3.1 (Java-time support built-in via `#temporals`) |

No Spring Security — no CSRF tokens needed on forms.

---

## Build, Test, and Run — Validated Commands

**Compile only** (fast check, ~5 s):
```bash
cd /home/paul/Trainbeans/ClearanceCard
./mvnw compile
```

**Run all 16 tests** (~15 s, always passes clean):
```bash
./mvnw test
# or from scratch:
./mvnw clean test
```

**Run the application:**
```bash
./mvnw spring-boot:run
```
App is ready when port 8080 responds. Startup takes ~20 s on this machine.

> **Never** add `spring.jackson.serialization.write-dates-as-timestamps` to `application.properties` — that enum was removed in Jackson 3.x and will crash the context.

---

## Package Layout

```
src/main/java/org/trainbeans/clearancecard/
  ClearanceCardApplication.java          ← @SpringBootApplication entry point
  config/    WebConfig.java              ← redirects / → /ui/clearance-cards
  controller/
    ClearanceCardController.java         ← @Controller, JSON API + print/receipt views
    UiController.java                    ← @Controller, browser UI (GET list/form, POST create)
  exception/ ClearanceCardNotFoundException.java
  model/     ClearanceCard.java          ← Java record (immutable response)
             ClearanceCardRequest.java   ← Java record (inbound DTO)
  repository/ClearanceCardRepository.java ← JdbcTemplate + SimpleJdbcInsert
  service/   ClearanceCardService.java

src/main/resources/
  application.properties                 ← app.railroad.name, datasource URL, H2 console
  schema.sql                             ← DDL; run every startup via spring.sql.init.mode=always
  static/css/ui.css                      ← shared stylesheet for UI pages
  templates/
    clearance-card-print.html            ← full-page Form 427-A print view
    clearance-card-receipt.html          ← 58 mm thermal receipt print view (@page size: 58mm auto)
    ui/card-list.html                    ← list + date-filter page
    ui/card-form.html                    ← new-card entry form

src/test/java/org/trainbeans/clearancecard/
  ClearanceCardApplicationTests.java     ← @SpringBootTest context-loads smoke test
  controller/ClearanceCardControllerTest.java  ← @WebMvcTest, 7 tests
  repository/ClearanceCardRepositoryTest.java  ← @JdbcTest, 8 tests
```

---

## Endpoints

| Method | Path | Type | Notes |
|--------|------|------|-------|
| `GET` | `/` | redirect | → `/ui/clearance-cards` |
| `GET` | `/ui/clearance-cards` | HTML | list; optional `?date=yyyy-MM-dd` |
| `GET` | `/ui/clearance-cards/new` | HTML | entry form |
| `POST` | `/ui/clearance-cards` | form submit | saves card, redirects to list |
| `POST` | `/api/clearance-cards` | JSON | creates card, returns 201 + Location |
| `GET` | `/api/clearance-cards` | JSON | all cards; optional `?date=` |
| `GET` | `/api/clearance-cards/{id}` | JSON | single card |
| `GET` | `/api/clearance-cards/{id}/print` | HTML | full-page Form 427-A |
| `GET` | `/api/clearance-cards/{id}/receipt` | HTML | 58 mm receipt layout |
| `GET` | `/h2-console` | HTML | JDBC URL: `jdbc:h2:mem:clearancecard` |
| `GET` | `/actuator/health` | JSON | health check |

---

## Key Architecture Rules
- **`ClearanceCardController`** is `@Controller` (not `@RestController`); JSON endpoints carry `@ResponseBody`; view endpoints return a template name string.
- **`ClearanceCardRepository`** uses `SimpleJdbcInsert` for inserts. The `clearance_card_order_number` insert uses `.usingColumns(...)` — never insert `id` explicitly (it is `GENERATED ALWAYS AS IDENTITY`).
- **N+1 avoidance**: `fetchWithChildren()` loads all child order-number rows in one `IN`-clause query; do not replace this with per-row lookups.
- **`orderCount`** is the operator's stated count, stored as-is. Do not validate it against `orderNumbers.size()`.
- Order number boxes and the signal-stop field are suppressed in templates when empty using `th:unless="${#lists.isEmpty(...)}"` / `th:unless="${#strings.isEmpty(...)}"`.
- Time is stored as a single `TIME` column; date as a single `DATE` column. Format times with `#temporals.format(card.issuedTime, 'h:mm a')` in templates.

## Spring Boot 4 Test Imports (differ from Boot 3)
```java
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;   // not boot.test.autoconfigure.web.servlet
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;        // not boot.test.autoconfigure.jdbc
import org.springframework.test.context.bean.override.mockito.MockitoBean; // not boot.test.mock.mockito
import tools.jackson.databind.ObjectMapper;                               // not com.fasterxml.jackson
```
`@WebMvcTest` slices **do not** load `UiController` — scope it explicitly: `@WebMvcTest(ClearanceCardController.class)`.  
`@JdbcTest` slices require `@Import(ClearanceCardRepository.class)` to pull in the repository bean.  
All tests also need `@TestPropertySource(properties = "app.railroad.name=Test Railroad")` when the railroad name is required.

## CI / Pre-Commit Validation
Two GitHub Actions workflows run on every push to `master` and every PR targeting `master`:

| Workflow | File | Trigger | Purpose |
|----------|------|---------|---------|
| CI | `.github/workflows/ci.yml` | push/PR → `master` | `./mvnw clean test`; uploads Surefire reports |
| Release | `.github/workflows/release.yml` | push → `master` or `v*.*.*` tag | builds & pushes Docker image to `ghcr.io` |

The Docker image is published to `ghcr.io/<owner>/<repo>`. Tags produced:
- `master` + `sha-<short>` on every merge to master
- `1.0.0`, `1.0`, `sha-<short>` when a `v1.0.0` git tag is pushed

The `Dockerfile` is a two-stage build (build JDK → runtime JRE). Docker Compose integration is disabled via `spring.docker.compose.enabled=false` in `application.properties`.

Always verify locally before pushing:
```bash
./mvnw clean test   # must show: Tests run: 16, Failures: 0, Errors: 0
```

