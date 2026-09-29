# Phase 1–2 Audit — Insurance AI Assistant

Audit date: local session, no external network/build tools available (see "How this audit was performed" below).

## How this audit was performed

This environment has Java 21 installed but **no Maven, no cached Maven dependencies, no network access, and no MySQL server**. That means `mvn test`, `mvn package`, and starting the actual Spring Boot app could not be executed here.

Instead, every file in the project was opened and read directly, and the code was traced by hand: constructors, field names, method calls, and imports were cross-checked file-by-file to confirm they actually line up (e.g. that a DTO field a service reads actually exists, that a repository method a service calls is actually declared). This is a real code review, not a guess — but it is **not** a substitute for actually compiling and running the app. That step still needs to happen on your machine (see the "Not verified" list at the end).

---

## 1. What actually exists

Confirmed by listing the repository directly — no frontend, no security, no AI/RAG, no Docker, no CI files exist. Only these:

```
pom.xml
.env.example
.gitignore
README.md
src/main/java/com/insurance/assistant/
    InsuranceAssistantApplication.java
    controller/  ClaimController, PolicyController, UserController
    dto/         ClaimResponse, CreateClaimRequest, PolicyResponse, RegisterUserRequest, UserResponse
    entity/      Claim, ClaimStatus, InsurancePlan, Policy, PolicyStatus, User
    exception/   DuplicateEmailException, ErrorResponse, GlobalExceptionHandler, ResourceNotFoundException
    repository/  ClaimRepository, InsurancePlanRepository, PolicyRepository, UserRepository
    service/     ClaimService, PolicyService, UserService
src/main/resources/
    application.properties
    data.sql
src/test/java/com/insurance/assistant/
    dto/CreateClaimRequestValidationTest.java
    service/ClaimServiceTest.java
    service/PolicyServiceTest.java
```

## 2. Phase 1 status — CONFIRMED PRESENT

- Java 17 (`pom.xml` `<java.version>17</java.version>`), Spring Boot 3.3.4 parent.
- `User`, `InsurancePlan`, `Policy`, `Claim` entities all exist with `@Entity`/`@Table` mappings.
- `UserRepository`, `PolicyRepository`, `ClaimRepository`, `InsurancePlanRepository` all extend `JpaRepository`.
- `UserService`, `PolicyService`, `ClaimService` exist and hold the actual logic.
- `UserController`, `PolicyController`, `ClaimController` are thin — they only call a service and return a response, no business logic in controllers.
- DTOs exist for input (`RegisterUserRequest`, `CreateClaimRequest`) and output (`UserResponse`, `PolicyResponse`, `ClaimResponse`) — entities are never returned directly from a controller.
- `@Valid` + Bean Validation annotations (`@NotBlank`, `@Email`, `@DecimalMin`, `@Size`) are used on both request DTOs.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) centralizes error handling for not-found, duplicate email, validation failures, and a catch-all.

**Structure check:** `Controller → Service → Repository` is followed consistently. No controller talks to a repository directly, no service contains HTTP-level code.

**APIs confirmed by reading the controller code:**

| Endpoint | Confirmed |
|---|---|
| `POST /api/users/register` | ✅ present, `@Valid`, returns 201 + `UserResponse`, 409 on duplicate email |
| `GET /api/policies/{id}` | ✅ present, 404 via `ResourceNotFoundException` |
| `GET /api/claims/{id}` | ✅ present, 404 via `ResourceNotFoundException` |
| `POST /api/claims` | ✅ present, `@Valid`, looks up the policy first, 404 if missing, 201 on success |

## 3. Phase 2 status — CONFIRMED PRESENT, with one gap found and fixed

- `application.properties` reads `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` via `${VAR:default}` syntax — no hardcoded credentials.
- `.env.example` exists with placeholder values only (`your_mysql_password`, not a real one).
- `.gitignore` ignores `.env` — confirmed by reading the file directly.
- JPA relationships: `Policy → User` (`@ManyToOne`), `Policy → InsurancePlan` (`@ManyToOne`), `Claim → Policy` (`@ManyToOne`) — all one-directional, no back-reference collections, so no bidirectional-JSON-recursion risk.
- `data.sql` seed data is internally consistent: both sample policies point at real users and plans, both sample claims point at real policies (verified by reading the actual INSERT statements and matching the IDs by hand).
- Validation: `@NotBlank`/`@Email` on user fields, `@DecimalMin("0.01")` on claim amount, `@NotBlank` on claim type/description — all present in the DTOs, not just in the entities.
- Database constraints: `unique = true` on `email`, `policyNumber`, `claimNumber`; `nullable = false` on required fields.

**Gap found:** `User.phone` exists on the entity and in `data.sql`'s seed data, and a 4-argument `User` constructor accepting phone already existed — but `RegisterUserRequest` had no `phone` field, so a real user registering through the API could never actually set their phone number. The constructor was dead code. **Fixed** — see below.

## 4. Tests — what exists, and what I could/couldn't verify

**Could not run `mvn test`** — no Maven, no network in this sandbox. What follows is a manual trace of each test against the code it exercises, not an execution result.

9 test methods across 3 files:

| File | Tests | Manually traced result |
|---|---|---|
| `PolicyServiceTest` | `returnsPolicyWhenItExists`, `throwsWhenPolicyDoesNotExist` | Logic matches `PolicyService.getPolicyById` exactly — should pass |
| `ClaimServiceTest` | `returnsClaimWhenItExists`, `throwsWhenClaimDoesNotExist`, `createsClaimForAnExistingPolicy`, `doesNotCreateClaimWhenPolicyIsMissing` | Logic matches `ClaimService` exactly, including the claim-number prefix (`"CLM-"`) — should pass |
| `CreateClaimRequestValidationTest` | `rejectsZeroOrNegativeClaimAmount`, `rejectsBlankDescription`, `acceptsAValidRequest` | Matches the `@DecimalMin`/`@NotBlank` annotations on `CreateClaimRequest` — should pass |

None of these tests touch a real database or a real LLM, so they don't need MySQL or an API key to run — that part of the Phase 2 spec is satisfied.

**You need to actually run `mvn test` yourself** and tell me the real output — I have not executed it.

## 5. Security check (secrets only — no Spring Security/JWT added)

Searched every tracked file for hardcoded secrets:

- `application.properties` — no password/key literals, only `${VAR:default}` placeholders with an **empty** default password, not a real one.
- `.env.example` — placeholder values only (`your_mysql_password`).
- `data.sql` — seed users have `password123` in plaintext. This is **not a leaked secret** (it's fake demo data, not a real credential), but it does confirm passwords are stored in plaintext in the database right now — expected at this stage, since password hashing is explicitly a later (security) phase, not Phase 1–2.
- No API keys anywhere — none were expected yet either, since LLM integration hasn't been built.
- `.gitignore` correctly excludes `.env`.

**No accidental secrets found.**

## 6. Fixes made this session

1. **`RegisterUserRequest`** — added a `phone` field (optional, no validation constraint) so the phone number that the `User` entity and seed data already support can actually be set through the API.
2. **`UserService`** — now calls the existing 4-argument `User` constructor with `request.getPhone()` instead of the 3-argument one, so phone is actually persisted.
3. **`UserResponse`** — added `phone` to the response so a caller can see what was saved.
4. **`GlobalExceptionHandler`** — the catch-all `Exception` handler correctly hides the stack trace from the client, but it was **silently swallowing the real exception with no logging at all** — there was no logger anywhere in the codebase. Added an SLF4J `Logger` and `log.error("Unhandled exception", ex)` before returning the generic 500. This uses `org.slf4j` which Spring Boot already brings in by default — no new dependency added.
5. Fixed a stale comment referencing "Phase 8" for the security phase — the phase-numbering docs you pasted later renumbered security to Phase 7, so the comment was corrected to be phase-number-agnostic.

I did **not** touch anything else — no renames, no restructuring, no new endpoints, no new business rules.

## 7. Remaining issues (found, not fixed — out of Phase 1–2 scope)

- `ClaimService.createClaim` doesn't check `policy.getStatus()` before allowing a claim — a claim can currently be filed against a `CANCELLED` or `EXPIRED` policy. This wasn't part of the Phase 1–2 spec (it shows up as a requirement in the later AI-tool phase), so I left it alone rather than inventing a new business rule mid-audit. Flagging it so it's a deliberate decision for a later phase, not a surprise.
- `generateClaimNumber()` picks a random 4-digit number; two claims created in the same millisecond in the same year have a small chance of colliding on the unique `claim_number` constraint. If that happens, it would currently surface as a generic 500 (now at least logged, per fix #4) rather than a clean error message. Low probability, not fixed — a real fix (retry-on-collision or a DB sequence) is more machinery than this stage needs.
- `findByUserId` (`PolicyRepository`), `findByPolicyNumber`, `findByClaimNumber`, `findByPolicyId`, `findByEmail` are all declared but **not called by any code yet**. Per the Phase 2 spec these were added ahead of need (e.g. `findByEmail` will matter once login exists). Left in place, not removed.
- `PolicyResponse` doesn't expose `coverageAmount` or `premiumAmount` from the linked plan, even though the Phase 3 frontend spec you shared expects the policy page to show them. Not a Phase 1–2 bug (Phase 1–2 never asked for those fields in the response) — just flagging it now so it isn't a surprise when Phase 3 starts.
- No `UserServiceTest` exists (only `PolicyServiceTest`/`ClaimServiceTest`). The Phase 2 spec's minimum test list didn't include registration, so this isn't a gap against what was asked — but it's worth knowing before an interviewer asks "did you test registration?"

## 8. Features intentionally NOT implemented yet

Confirmed absent by file inspection — consistent with what you asked me not to build yet:

- No frontend (`frontend/` doesn't exist)
- No Spring Security / JWT / authentication or authorization
- No Spring AI / LLM integration
- No RAG / pgvector / PostgreSQL
- No AI agent / tool calling
- No Docker / `docker-compose.yml` / `.dockerignore`
- No GitHub Actions / `.github/workflows/`
- No monitoring / Actuator

## 9. Not verified (you need to do this and report back)

- `mvn test` actually running and passing
- The app actually starting against a real MySQL instance
- Hibernate actually creating the schema correctly from the entities
- `data.sql` actually inserting without errors
- The 4 endpoints actually returning correct responses over HTTP
- Whether `mvn package` produces a working JAR

## 10. Final Phase 1–2 status

**Structurally complete and internally consistent** based on full manual code review — architecture, DTOs, validation, exception handling, and relationships all match what Phase 1 and Phase 2 asked for, one real gap (unregisterable phone number) and one real robustness issue (unlogged exceptions) were found and fixed, and several forward-looking items are documented rather than silently left unexplained.

**Not yet confirmed working** — this audit could not execute the code in this environment. The next real step is for you to run `mvn test` and `mvn spring-boot:run` against a real MySQL instance and report back the actual output, so we can close the loop on section 9 before calling Phase 1–2 fully verified.
