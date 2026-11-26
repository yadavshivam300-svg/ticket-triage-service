# AI Verification Log

This log documents how GitHub Copilot output was audited during the development of `ticket-triage-service`, including edge cases identified through manual code review and runtime testing.

## Logged Findings

### 1. Substring Matching False Positives in `KeywordTriageStrategy`
* **Prompt / Task:** Generate a case-insensitive, null-safe `KeywordTriageStrategy` classifying tickets across `SECURITY`, `TECHNICAL`, `BILLING`, and `GENERAL` in strict priority order.
* **What Was Risky in Copilot's Output:** Copilot implemented keyword detection using `String.contains(keyword)`. While null-safe and properly ordered (`SECURITY` evaluated before `TECHNICAL` and `BILLING`), substring matching triggers false positives on unrelated words—for example, `"Cannot download PDF"` matches `"down"` (`TECHNICAL`/`HIGH`), and `"Order #5000"` matches `"500"` (`TECHNICAL`/`HIGH`).
* **Assessment & Planned Mitigation:** Logged as a known limitation of the baseline keyword strategy. Production hardening would replace `String.contains(...)` with precompiled word-boundary regex patterns (`\b<keyword>\b`) or swap in a semantic LLM-based `TriageStrategy` implementation.

## Baseline Controls Audited & Verified

Across all feature branches (`feat/ticket-crud`, `feat/triage-engine`, `feat/testing-and-coverage`), Copilot-generated code was manually audited and smoke-tested against the following standards before merging:

* **Framework Compatibility:** Verified `jakarta.*` imports (no legacy `javax.*`) and Spring Boot 4 `@MockitoBean` / `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` usage.
* **REST Semantics & Error Contracts:** Verified `201 Created` with `Location` header on `POST`, `204 No Content` on `DELETE`, `404 Not Found` when deleting missing IDs (avoiding Spring Data JPA's silent-ignore behavior), `409 Conflict` on illegal state transitions, and `400 Bad Request` on invalid enum JSON payloads.
* **Domain Encapsulation:** Verified no public `setStatus` setter remained on `Ticket`, forcing all status mutations through `Ticket.transitionTo(...)`.
* **Test Quality:** Verified every JUnit 5, Mockito, and `MockMvc` test asserted concrete state, interaction order (`InOrder`), and JSON error fields rather than padding coverage metrics.