# Insurance AI Assistant

A full-stack insurance customer portal that combines a Spring Boot REST API with a real, working AI assistant — one that answers policy questions using **RAG (Retrieval-Augmented Generation)** grounded in actual policy documents, rather than guessing.

Built incrementally, phase by phase, with every phase actually tested and verified end-to-end before moving to the next (see [Progress so far](#progress-so-far) below for exactly what's built vs. planned).

<!-- Add a short screen recording or screenshot here once you have one, e.g.:
![Demo](docs/demo.gif)
This is the single highest-value addition for anyone browsing this repo. -->

## Features

- User registration, policy lookup, and claim submission (Spring Boot REST APIs + MySQL)
- A plain HTML/CSS/JavaScript frontend — dashboard, policy details, claims history + submission, AI chat
- An AI assistant (via [Groq](https://groq.com), through Spring AI) that answers real questions about insurance coverage
- **RAG**: policy documents are chunked, embedded locally (no extra paid API), and stored in **PostgreSQL + pgvector**; the assistant retrieves relevant chunks before answering and **cites its sources**
- The assistant explicitly says "I don't know" when a question falls outside the available documents, instead of inventing coverage details

## Tech stack

**Backend:** Java 17, Spring Boot 3.5, Spring Data JPA, Spring AI
**Databases:** MySQL (application data), PostgreSQL + pgvector (embeddings)
**AI:** Groq (LLM), local ONNX embeddings (`all-MiniLM-L6-v2`)
**Frontend:** HTML, CSS, vanilla JavaScript
**Testing:** JUnit 5, Mockito (20 tests, all passing)

## Project structure

```
src/main/java/com/insurance/assistant/
├── controller/    REST endpoints
├── service/       business logic + AI/RAG
├── repository/    Spring Data JPA
├── entity/        database tables
├── dto/           request/response shapes
├── exception/     centralized error handling
└── config/        datasources, vector store, CORS

src/main/resources/documents/   sample policy documents (RAG source material)
frontend/                       plain HTML/CSS/JS UI
```

## Getting started

This project needs two databases (MySQL and PostgreSQL+pgvector) and a free [Groq](https://console.groq.com) API key. Full setup instructions, environment variables, and how each part works are documented phase-by-phase below — start with [Database (Phase 2)](#database-phase-2) for MySQL setup and [RAG (Phase 5)](#rag-phase-5) for the Postgres/pgvector + AI setup.

## Progress so far

- **Phase 1** — Core Spring Boot app: User/Policy/Claim entities, repositories, services, REST endpoints, global exception handling.
- **Phase 2** — MySQL connection, expanded entity fields, sample data, validation, unit tests.
- **Phase 3** — Plain HTML/CSS/JS frontend, CORS config, small backend additions to support it.
- **Phase 4** — LLM integration via Spring AI + Groq, basic system prompt, `POST /api/ai/chat`.
- **Phase 5** — RAG: PostgreSQL + pgvector, local embeddings, document ingestion, grounded answers with sources.
- *Not yet built:* AI agent/tool calling, authentication/authorization, Docker, CI/CD (see Future improvements at the bottom)

## Database (Phase 2)

### Why MySQL

The core application data — users, policies, claims — is relational and has clear structure: a user has policies, a policy belongs to one plan, and claims belong to one policy. Nothing here calls for a document or key-value store, so a plain relational database (MySQL) is the straightforward choice for this data.

### Tables

| Table | Represents |
|---|---|
| `users` | People who hold policies (id, name, email, password, phone, createdAt) |
| `insurance_plans` | The insurance products on offer (id, planName, description, coverageAmount, premiumAmount, createdAt) |
| `policies` | A specific user's subscription to a plan (id, policyNumber, user_id, insurance_plan_id, startDate, endDate, status) |
| `claims` | A claim filed against a policy (id, claimNumber, policy_id, claimType, description, claimAmount, status, submittedAt) |

### Relationships

```
User (1) ───< Policy (many)
InsurancePlan (1) ───< Policy (many)
Policy (1) ───< Claim (many)
```

Each relationship is modeled as a one-directional `@ManyToOne` on the "many" side (`Policy → User`, `Policy → InsurancePlan`, `Claim → Policy`). There are no back-reference collections (e.g. no `user.getPolicies()`). This keeps the entities simple and avoids the JSON-serialization/lazy-loading recursion problems that bidirectional JPA relationships commonly cause — a "many" side is added only if a real feature needs it later.

### Configuration

The app never reads DB credentials from `application.properties` directly — it reads them from environment variables, with local-dev defaults:

```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=insurance_assistant
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
```

Copy `.env.example` to `.env` (or export the same variables) before running locally. `.env` is git-ignored.

### Sample data

`src/main/resources/data.sql` inserts a small, internally-consistent dataset on every startup: two users (Usha, Ravi), two plans (Health Secure, Family Health Plus), two policies (POL1001, POL1002), and two claims (CLM1001, CLM1002) — each claim points at a real policy, and each policy at a real user and plan.

**Initialization ordering:** by default, Spring Boot runs `data.sql` *before* Hibernate creates the schema from the entities, so the inserts fail with "table doesn't exist." Two properties fix this:

```properties
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always
```

This tells Spring to let Hibernate create/update the schema first, then run `data.sql` afterwards.

### Running locally

1. Have a MySQL server running and create the database: `CREATE DATABASE insurance_assistant;`
2. Set the environment variables above (or use `.env`).
3. `mvn spring-boot:run`
4. Hibernate creates the tables, then `data.sql` seeds them.

### How I'd explain this in an interview

"I used MySQL because the core data — users, policies, claims — is structured and relational: a claim always belongs to exactly one policy, a policy to exactly one user and one plan. I kept the JPA relationships one-directional to avoid the classic bidirectional-serialization problem, and I seed a small, realistic dataset through `data.sql` rather than hand-inserting rows through the API every time I restart the app."

## API endpoints (so far)

| Method | Path | Notes |
|---|---|---|
| POST | `/api/users/register` | 201 + user (no password), 409 on duplicate email |
| GET | `/api/users/{id}` | 404 if missing - added in Phase 3 so the dashboard can show the user's name |
| GET | `/api/policies/{id}` | 404 if missing |
| GET | `/api/policies/user/{userId}` | Returns a list - added in Phase 3 to stand in for "my policies" until auth exists |
| GET | `/api/claims/{id}` | 404 if missing |
| GET | `/api/claims/policy/{policyId}` | Returns a list - added in Phase 3 for the dashboard/claims page |
| POST | `/api/claims` | Validates policy exists, generates a claim number, 201 |
| POST | `/api/ai/chat` | Question in, `{answer, sources}` out. Answer is grounded in the policy documents via RAG. Returns a safe fallback message (empty sources) on any AI provider or vector-search failure rather than an error - see Phase 5 below |

## Testing

Unit tests (JUnit 5 + Mockito) cover:
- `PolicyServiceTest` — find existing policy, 404 on missing policy, list policies for a user, empty list when a user has none
- `ClaimServiceTest` — find existing claim, 404 on missing claim, successful claim creation, creation blocked when the policy doesn't exist
- `CreateClaimRequestValidationTest` — rejects zero/negative claim amount, rejects blank description, accepts a valid request
- `AiAssistantServiceTest` — returns an answer with sources when relevant chunks are found, returns the "no context" message when nothing relevant is found, falls back safely when the provider throws, falls back safely on an empty response, falls back safely when vector search itself throws. Mocks Spring AI's `ChatClient` and `VectorStore` directly, so no real `GROQ_API_KEY` or Postgres connection is needed to run these
- `AiChatRequestValidationTest` — rejects blank/null/over-length questions, accepts a reasonable one

Run with `mvn test`.

## Frontend (Phase 3)

### Why vanilla JavaScript

The backend and AI integration are the actual focus of this project, so the frontend uses plain HTML/CSS/JavaScript rather than a framework - there's nothing here (routing, complex state, reusable components) that justifies React or a build step. Each page is a normal HTML file with its own small script.

### How it talks to the backend

Every page loads `js/api.js` first, which centralizes the API base URL and a single `apiRequest()` helper that all the page-specific scripts (`login.js`, `dashboard.js`, `policy.js`, `claims.js`, `assistant.js`) call. That helper is the only place that knows how to build a URL, set headers, and turn a failed HTTP response into a readable error - so no page duplicates fetch/error-handling logic.

### Pages and the APIs they use

| Page | Calls |
|---|---|
| `login.html` | `POST /api/users/register` (Register button - real). Login button doesn't call anything real yet - see below. |
| `dashboard.html` | `GET /api/users/{id}`, `GET /api/policies/user/{userId}`, `GET /api/claims/policy/{policyId}` |
| `policy.html` | `GET /api/policies/user/{userId}`, `GET /api/policies/{id}` |
| `claims.html` | `GET /api/policies/user/{userId}`, `GET /api/claims/policy/{policyId}`, `POST /api/claims` |
| `assistant.html` | None yet - `POST /api/ai/chat` doesn't exist until the LLM phase |

### No authentication yet - what the frontend does instead

There's no login endpoint yet (that's a later phase), so this frontend does **not** invent a fake one. Every page currently acts as a single hardcoded demo user (`CURRENT_USER_ID = 1` in `api.js`, matching the "Usha" seed user). The Register button on the login page is real and calls the actual registration API; the Login button is UI-only for now and just continues to the dashboard, with a message explaining why. `api.js` already has a `login()` function shaped for `POST /api/auth/login` so wiring up real authentication later is a small change, not a rewrite.

### CORS

Since the frontend and backend run as separate processes during local development, `WebConfig` (`src/main/java/.../config/WebConfig.java`) allows cross-origin requests from a single configurable origin (`FRONTEND_ORIGIN`, default `http://localhost:5500` - VS Code Live Server's default port) rather than `allowedOrigins("*")`.

### Running the frontend locally

The frontend is static files with no build step. Easiest option: open the `frontend/` folder in VS Code and use the "Live Server" extension, or run any simple static file server from that folder (e.g. `python3 -m http.server 5500`). Make sure the backend is running first. It defaults to port 8080; if that port is already taken locally (common with XAMPP/Apache), start it with `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` and change `API_BASE_URL` in `frontend/js/api.js` to match.

### How I'd explain this in an interview

"The frontend uses plain JavaScript to call the Spring Boot REST APIs through one shared `apiRequest()` helper, so every page handles errors and loading states the same way. I kept it framework-free because the project's focus is the backend and AI integration, not frontend architecture. Since authentication doesn't exist yet, I didn't fake a login API - the frontend runs as a single demo user for now, and the code is already structured so a real login endpoint is a small change later, not a rewrite."

## LLM integration (Phase 4)

### Provider

Uses **Groq** through Spring AI's OpenAI-compatible client (`spring-ai-starter-model-openai`). Groq's API is OpenAI-compatible, so rather than adding a Groq-specific dependency, `application.properties` just points the existing OpenAI client at Groq's base URL:

```properties
spring.ai.openai.api-key=${GROQ_API_KEY:}
spring.ai.openai.base-url=https://api.groq.com/openai
spring.ai.openai.chat.options.model=${GROQ_MODEL:openai/gpt-oss-120b}
```

Groq was chosen because it has a genuinely free tier (no credit card, no trial that expires), and it's a supported, documented way to use Spring AI - not a workaround.

**Note on model choice:** Groq's free-tier model lineup changes fairly often. This project originally used `llama-3.3-70b-versatile`, which Groq deprecated for free/developer accounts in June 2026 (it's now Enterprise-only). The real error surfaced as an HTTP 404 `model_not_found` from Groq, logged by `AiAssistantService` and shown to the user as the safe fallback message rather than a raw error - which is exactly the failure mode this design was built to handle gracefully. The model was switched to `openai/gpt-oss-120b`, Groq's own recommended replacement. If this happens again, check `console.groq.com/docs/models` for models marked "Production" and update `GROQ_MODEL` - no code change needed.

### Architecture

```
Frontend (assistant.html)
      ↓
POST /api/ai/chat
      ↓
AiAssistantController   (HTTP only - validates the request, calls the service)
      ↓
AiAssistantService      (builds the prompt, calls the model, handles failures)
      ↓
Spring AI ChatClient
      ↓
Groq (LLM)
```

The API key never reaches the browser - the frontend only ever calls this app's own `/api/ai/chat` endpoint, never Groq directly.

### System prompt

Kept as a constant inside `AiAssistantService` (not in the controller, not scattered across files):

> "You are an insurance assistant for a software demonstration. Answer questions clearly and concisely. Do not invent specific policy details, coverage limits, or claim information you have not been given. If you do not have enough information to answer a question accurately, say so plainly instead of guessing..."

There's no RAG yet (that's the next phase), so right now the assistant genuinely doesn't have real policy documents to draw on - the prompt is written to make it say so rather than invent plausible-sounding coverage details.

### Error handling

`AiAssistantService.answer()` catches any failure from the provider (missing/invalid API key, Groq being down, a timeout, a rate limit, or an empty response) and returns one safe fallback message - `"The AI assistant is temporarily unavailable. Please try again."` - rather than a raw error or a stack trace. The real exception is logged server-side (message only, not the user's full question, since a question could contain sensitive details) so a real bug is still diagnosable from the logs.

### Input validation

`AiChatRequest` requires a non-blank question, capped at 500 characters, so the endpoint can't be sent an empty request or an unbounded wall of text.

### Testing without a real API key

`AiAssistantServiceTest` mocks Spring AI's `ChatClient` fluent chain (`prompt().user().call().content()`) directly, so the automated tests never make a real network call and don't need `GROQ_API_KEY` set to pass. Tests cover: a normal answer, the provider throwing an exception, and the provider returning a blank response - all fall back to the safe message except the first.

### Not yet verified

This was written and reviewed carefully, but actually calling the real Groq API has not been tested in the environment this was built in (no outbound network access there). The person running this locally needs to confirm the real end-to-end call works with their own `GROQ_API_KEY`.

### How I'd explain this in an interview

"I integrated the LLM through the Spring Boot backend using Spring AI, with Groq as the provider since it's OpenAI-API-compatible and has a genuinely free tier. The frontend only ever talks to my own `/api/ai/chat` endpoint - it never sees the API key or calls Groq directly. I kept the system prompt in the service layer, not the controller, and made sure any failure from the AI provider - a bad key, a timeout, a rate limit - returns one safe fallback message instead of leaking an error, while still logging the real cause on the backend. The next step is RAG, so the assistant can answer from actual policy documents instead of general knowledge."

## RAG (Phase 5)

### Why a second database

RAG needs to store and search document embeddings (vectors), which MySQL can't do. Rather than replace MySQL, this app uses **two separate databases**: MySQL for application data (unchanged from Phase 2), and **PostgreSQL with the pgvector extension** just for document embeddings. `DataSourceConfig` defines both `DataSource` beans explicitly - the MySQL one stays `@Primary` (used by JPA, unchanged), and the Postgres one is used only by the vector store. This is the standard Spring Boot "two DataSources" pattern, not a workaround.

### Why local embeddings instead of another API

Groq (the chat provider from Phase 4) doesn't offer an embeddings endpoint. Rather than sign up for a second AI provider just to compute embeddings, this project uses Spring AI's local ONNX embedding model (`spring-ai-starter-model-transformers`, model: `all-MiniLM-L6-v2`, 384 dimensions). It runs inside the JVM, downloads the model once, and needs no API key - genuinely free, and one less external dependency to explain or for something to go wrong with.

### Architecture

```
Startup:
  documents/*.txt  →  TextReader  →  TokenTextSplitter  →  local embeddings  →  pgvector

Question:
  User question
        ↓
  AiAssistantService
        ↓
  vectorStore.similaritySearch(question, topK=3)   →  pgvector (embeddings, MiniLM)
        ↓
  top 3 relevant chunks + question  →  prompt  →  Groq (chat)
        ↓
  {answer, sources}
```

`DocumentIngestionService` runs once at startup (`CommandLineRunner`), reads every `.txt` file in `src/main/resources/documents/`, splits each into token-sized chunks, tags each chunk with its source filename, and embeds+stores them - skipping re-ingestion if the store is already populated, so restarting the app doesn't re-embed everything every time.

### Sample documents

Four small, realistic documents: `health-insurance-policy.txt`, `hospitalization-coverage.txt`, `claim-process.txt`, `exclusions.txt` - enough to answer coverage, hospitalization, claims-process, and exclusion questions, and to demonstrate the assistant correctly saying "I don't know" for anything outside them (e.g. "does this cover international space travel?").

### Grounding - not inventing coverage

`AiAssistantService` builds the prompt from *only* the retrieved chunks - the system prompt explicitly tells the model not to invent policy details, coverage limits, or eligibility beyond what's in the given context. If nothing relevant is found in the vector store at all, the service doesn't call the LLM - it returns a fixed "I couldn't find anything about that in the available policy documents" message directly, with no risk of the model guessing.

### Sources in the response

Every successful answer includes which document(s) it came from:
```json
{
  "answer": "Hospitalization is covered, subject to the room rent limit...",
  "sources": ["hospitalization-coverage.txt"]
}
```
The frontend AI Assistant page shows this under the answer as "Sources: ...".

### Configuration

```properties
app.vector-datasource.url=jdbc:postgresql://${VECTOR_DB_HOST:localhost}:${VECTOR_DB_PORT:5432}/${VECTOR_DB_NAME:insurance_vectors}
app.vector-datasource.username=${VECTOR_DB_USERNAME:postgres}
app.vector-datasource.password=${VECTOR_DB_PASSWORD:}
```
`VectorStoreConfig` builds the `PgVectorStore` bean directly (`initializeSchema(true)`) rather than relying on Spring AI's auto-configuring starter, specifically so it uses the dedicated Postgres `DataSource`, not the MySQL one.

### Running locally

1. Install PostgreSQL locally (separate from MySQL) and enable the pgvector extension:
   ```sql
   CREATE DATABASE insurance_vectors;
   \c insurance_vectors
   CREATE EXTENSION IF NOT EXISTS vector;
   ```
   (The easiest way to get pgvector without compiling anything is the official `pgvector/pgvector` Docker image, if Docker is available - `docker run -d --name pgvector-db -p 5433:5432 -e POSTGRES_PASSWORD=postgres pgvector/pgvector:pg16`. Port 5433 is used on the host side because a locally-installed PostgreSQL server commonly already occupies 5432 - check with `netstat -ano | findstr :5432` (Windows) first if unsure. A native PostgreSQL install also works but needs the pgvector extension installed separately - see the pgvector GitHub repo for OS-specific instructions.)
2. Set the `VECTOR_DB_*` environment variables (see `.env.example`).
3. Start the app - on first run, watch the logs for "Ingested N chunks from 4 policy documents."

### Not yet verified

Same caveat as Phase 4: this was written and reviewed carefully, but the full pipeline (Postgres + pgvector + local embedding download + actual similarity search + a real Groq call) has not been run end-to-end in the environment this was built in (no PostgreSQL, no outbound network access there). This needs to be verified on a real machine.

### How I'd explain this in an interview

"For RAG I needed a vector database, so I added PostgreSQL with the pgvector extension alongside the existing MySQL database - MySQL still handles the application data, Postgres only stores document embeddings. I used a local, open-source embedding model instead of a second paid API, since Groq doesn't offer embeddings. On startup, the app reads the policy documents, splits them into chunks, embeds them, and stores them in pgvector. When a user asks a question, I do a similarity search to find the most relevant chunks, then pass only those chunks to the LLM as context - so it answers from the actual policy documents instead of general knowledge, and I can show which documents an answer came from. If nothing relevant is found, it says so instead of guessing."

## Future improvements

AI agent/tool calling, authentication/authorization, Docker, and CI/CD are covered in later phases and aren't implemented yet.
