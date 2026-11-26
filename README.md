# Ticket Triage Service

A modular Spring Boot REST microservice that ingests IT and customer support tickets, automatically classifies priority and category via a pluggable triage engine, and enforces strict ticket lifecycle transitions. Built with test-alongside-code discipline, automated JaCoCo coverage gating, and verification-first AI-assisted development.

## Tech Stack

* **Core:** Java, Spring Boot, Spring Data JPA, H2 (in-memory SQL), Jakarta Bean Validation
* **Testing & Quality:** JUnit 5, Mockito, Spring MockMvc (`@WebMvcTest`), JaCoCo (80% minimum coverage gate)
* **CI/CD & Tooling:** Maven Wrapper (`mvnw`), GitHub Actions, GitHub Copilot

## Architecture & LLD Highlights

* **Layered Modularity:** Strict separation across `controller`, `service`, `repository`, `domain`, `dto`, and `exception`. Java `record` DTOs decouple external API contracts from JPA entities, and `@RestControllerAdvice` centralizes structured JSON error responses (`400`, `404`, `409`).
* **Pluggable Triage Engine (Strategy Pattern):** `TicketService` depends on the `TriageStrategy` interface inside `domain.strategy` rather than concrete classification logic. `KeywordTriageStrategy` evaluates rules in priority order (`SECURITY` > `TECHNICAL` > `BILLING` > `GENERAL`), allowing future LLM-based classifiers (e.g., via SAP AI Core) to be swapped in without modifying service code.
* **Ticket Lifecycle State Machine (Enum-Driven):** Valid transitions are encapsulated directly inside `TicketStatus.canTransitionTo(...)` and enforced via `Ticket.transitionTo(...)` (with no public `setStatus` setter). Illegal transitions throw `InvalidStatusTransitionException`, mapped to `409 Conflict`.
  * `OPEN` -> `IN_PROGRESS`, `CLOSED`
  * `IN_PROGRESS` -> `RESOLVED`, `OPEN`, `CLOSED`
  * `RESOLVED` -> `CLOSED`, `IN_PROGRESS`
  * `CLOSED` -> *(terminal state)*

## REST API Endpoints

| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/tickets` | Create and auto-triage a ticket | `201 Created` (with `Location` header), `400 Bad Request` |
| `GET` | `/api/v1/tickets` | List all tickets (optional `?status=` filter) | `200 OK`, `400 Bad Request` |
| `GET` | `/api/v1/tickets/{id}` | Fetch a ticket by ID | `200 OK`, `404 Not Found` |
| `PATCH` | `/api/v1/tickets/{id}/status` | Transition ticket status via state machine | `200 OK`, `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `DELETE` | `/api/v1/tickets/{id}` | Delete a ticket by ID | `204 No Content`, `404 Not Found` |

## Running Tests & Coverage Quality Gate

Run the full test suite and enforce the 80% JaCoCo coverage gate locally:

```bash
# macOS / Linux
./mvnw verify

# Windows
mvnw.cmd verify
```

The build achieves **97% instruction coverage and 100% branch coverage** across unit and `MockMvc` slice tests, and runs automatically on every push and pull request via GitHub Actions.

## AI-Assisted Development & Verification

GitHub Copilot was used in interactive/plan mode during development, with every generated class and test manually audited for framework compatibility, REST semantics, and edge cases. See [`AI_VERIFICATION_LOG.md`](./AI_VERIFICATION_LOG.md) for documented audit checks and edge-case findings.