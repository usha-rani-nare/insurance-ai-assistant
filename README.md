# Insurance AI Assistant

![CI](https://github.com/usha-rani-nare/insurance-ai-assistant/actions/workflows/ci.yml/badge.svg)

A full-stack insurance customer portal that combines a Spring Boot REST API with a working AI assistant. The assistant answers policy questions using **RAG (Retrieval-Augmented Generation)** grounded in real policy documents, and it can **look up policies, check claim status and file claims** through tool calling, always limited to the logged-in customer's own data.

Built incrementally, phase by phase, with each phase tested end-to-end before moving to the next (see [Progress so far](#progress-so-far) for what is built and what is still planned).

<!-- Add a short screen recording or screenshot here once you have one, e.g.:
![Demo](docs/demo.gif)
This is the single highest-value addition for anyone browsing this repo. -->

## Features

- User registration and login with **BCrypt-hashed passwords** and **JWT authentication**
- Policy lookup and claim submission (Spring Boot REST APIs + MySQL), with **per-user authorization**: a user can only see their own policies, claims and profile
- A plain HTML/CSS/JavaScript frontend: login, dashboard, policy details, claims history and submission, AI chat
- An AI assistant (via [Groq](https://groq.com), through Spring AI) with separate system prompts for policy questions, claim questions and out-of-scope questions
- **RAG**: policy documents are chunked, embedded locally (no extra paid API) and stored in **PostgreSQL + pgvector**; the assistant retrieves relevant chunks before answering and **cites its sources**
- **Tool calling**: the assistant can call `getPolicyDetails`, `getClaimStatus` and `createClaim`. Each tool only works on the logged-in user's own data, so the model cannot be prompted into reading or filing against someone else's policy
- The assistant says "I don't know" when a question falls outside the available documents, instead of inventing coverage details
- **Docker Compose** setup that runs the backend, MySQL and pgvector Postgres together
- **GitHub Actions CI** that runs the tests and builds the Docker image on every push and pull request

## Tech stack

**Backend:** Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security, Spring AI, jjwt
**Databases:** MySQL (application data), PostgreSQL + pgvector (embeddings)
**AI:** Groq (LLM), local ONNX embeddings (`all-MiniLM-L6-v2`)
**Frontend:** HTML, CSS, vanilla JavaScript
**Testing:** JUnit 5, Mockito, Spring MockMvc (41 tests, all passing)
**DevOps:** Docker, Docker Compose, GitHub Actions

## Project structure

```
src/main/java/com/insurance/assistant/
├── controller/    REST endpoints (users, policies, claims, auth, AI chat)
├── service/       business logic, AI/RAG, prompt templates, AI tools
├── security/      JWT creation/validation, JWT request filter, current-user helper
├── repository/    Spring Data JPA
├── entity/        database tables
├── dto/           request/response shapes
├── exception/     centralized error handling
└── config/        security rules, password hashing, datasources, vector store, CORS

src/main/resources/documents/   sample policy documents (RAG source material)
src/test/java/                  unit and web-layer tests
frontend/                       plain HTML/CSS/JS UI
Dockerfile, docker-compose.yml  container setup
.github/workflows/ci.yml        CI pipeline
```

## Getting started

You need a free [Groq](https://console.groq.com) API key. There are two ways to run the backend.

### Option 1: Docker (recommended)

Requires Docker Desktop. This starts the backend, MySQL and the pgvector Postgres together.

1. Copy `.env.example` to `.env` and fill in the values:

   | Variable | Meaning |
   |---|---|
   | `GROQ_API_KEY` | Your Groq API key |
   | `JWT_SECRET` | A random string of at least 32 characters used to sign tokens |
   | `DB_PASSWORD` | Password for the MySQL container (you choose it) |
   | `VECTOR_DB_PASSWORD` | Password for the Postgres container (you choose it) |

2. Start everything:

   ```
   docker compose up -d --build
   ```

3. The API is at `http://localhost:8081`. Serve the `frontend/` folder with any static server (for example the VS Code Live Server extension on port 5500) and open `login.html`.
4. Demo login (created from `data.sql`): `usha@example.com` / `password123`.

| Command | What it does |
|---|---|
| `docker compose logs -f backend` | Follow the backend logs |
| `docker compose down` | Stop everything, keep the data |
| `docker compose down -v` | Stop everything and delete the databases |

MySQL is exposed on host port 3307 and Postgres on 5434, so they do not clash with databases already installed locally. The policy documents are ingested into pgvector automatically on the first start.

### Option 2: Run locally

1. Start MySQL and create the database: `CREATE DATABASE insurance_assistant;`
2. Start PostgreSQL with pgvector (see [RAG (Phase 5)](#rag-phase-5) for the Docker one-liner).
3. Set the environment variables (see `.env.example`), including `JWT_SECRET` and `GROQ_API_KEY`.
4. `mvn spring-boot:run`

The backend listens on port **8081**.

## Progress so far

- **Phase 1**: Core Spring Boot app: User/Policy/Claim entities, repositories, services, REST endpoints, global exception handling.
- **Phase 2**: MySQL connection, expanded entity fields, sample data, validation, unit tests.
- **Phase 3**: Plain HTML/CSS/JS frontend, CORS config, small backend additions to support it.
- **Phase 4**: LLM integration via Spring AI + Groq, `POST /api/ai/chat`.
- **Phase 5**: RAG: PostgreSQL + pgvector, local embeddings, document ingestion, grounded answers with sources.
- **Phase 6**: Prompt engineering: separate system prompts for policy, claim and out-of-scope questions, with simple routing.
- **Phase 7**: AI agent with tool calling (`getPolicyDetails`, `getClaimStatus`, `createClaim`).
- **Phase 8**: Security: BCrypt password hashing, JWT login, per-user authorization (including the AI tools).
- **Phase 9**: Testing: JWT, password hashing, AI tool ownership and HTTP-level security tests (41 tests in total).
- **Phase 10**: Docker and Docker Compose.
- **Phase 11**: CI with GitHub Actions.
- *Not yet built:* monitoring (Spring Boot Actuator), a Postman collection, and the items under [Known limitations](#known-limitations-and-future-improvements).

## Database (Phase 2)

### Why MySQL

The core application data (users, policies, claims) is relational and has clear structure: a user has policies, a policy belongs to one plan, and claims belong to one policy. Nothing here calls for a document or key-value store, so a plain relational database is the straightforward choice.

### Tables

| Table | Represents |
|---|---|
| `users` | People who hold policies (id, name, email, password hash, phone, createdAt) |
| `insurance_plans` | The insurance products on offer (id, planName, description, coverageAmount, premiumAmount, createdAt) |
| `policies` | A specific user's subscription to a plan (id, policyNumber, user_id, insurance_plan_id, startDate, endDate, status) |
| `claims` | A claim filed against a policy (id, claimNumber, policy_id, claimType, description, claimAmount, status, submittedAt) |

### Relationships

```
User (1) ───< Policy (many)
InsurancePlan (1) ───< Policy (many)
Policy (1) ───< Claim (many)
```

Each relationship is a one-directional `@ManyToOne` on the "many" side (`Policy → User`, `Policy → InsurancePlan`, `Claim → Policy`). There are no back-reference collections, which keeps the entities simple and avoids the JSON-serialization and lazy-loading recursion problems that bidirectional JPA relationships commonly cause.

### Configuration

The app reads database settings from environment variables, with local-dev defaults:

```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=insurance_assistant
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
```

Copy `.env.example` to `.env` (or export the same variables) before running locally. `.env` is git-ignored.

### Sample data

`src/main/resources/data.sql` inserts a small, consistent dataset on every startup: two users (Usha, Ravi), two plans (Health Secure, Family Health Plus), two policies (POL1001, POL1002) and two claims (CLM1001, CLM1002).

By default Spring Boot runs `data.sql` before Hibernate creates the schema, so the inserts would fail. Two properties fix the order:

```properties
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always
```

The seed users are inserted with plain-text demo passwords. On startup, `PasswordMigrationRunner` replaces any password that is not already a BCrypt hash with its hash. This is a dev/demo convenience only; a real project would ship hashed values in a migration instead.

### How I'd explain this in an interview

"I used MySQL because the core data (users, policies, claims) is structured and relational: a claim always belongs to exactly one policy, a policy to exactly one user and one plan. I kept the JPA relationships one-directional to avoid the classic bidirectional-serialization problem, and I seed a small, realistic dataset through `data.sql` instead of inserting rows by hand every time I restart the app."

## API endpoints

All endpoints except login and registration require an `Authorization: Bearer <token>` header.

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Email + password in, `{token, userId, name, email}` out. 401 with the same message for a wrong password or an unknown email |
| POST | `/api/users/register` | Public | 201 + user (no password), 409 on duplicate email. Password is stored as a BCrypt hash |
| GET | `/api/users/{id}` | Token | Only your own profile, 403 otherwise |
| GET | `/api/policies/{id}` | Token | Only your own policies, 404 otherwise |
| GET | `/api/policies/user/{userId}` | Token | Only for your own user id, 403 otherwise |
| GET | `/api/claims/{id}` | Token | Only claims on your own policies, 404 otherwise |
| GET | `/api/claims/policy/{policyId}` | Token | Only for your own policies, 404 otherwise |
| POST | `/api/claims` | Token | Policy must belong to you (404 otherwise); generates a claim number, 201 |
| POST | `/api/ai/chat` | Token | Question in, `{answer, sources}` out. Grounded in the policy documents via RAG, with tool calling for live policy/claim data. Returns a safe fallback message on any AI provider or vector-search failure |

## Security (Phase 8)

- **Passwords** are hashed with BCrypt before they are stored. Plain-text passwords are never saved, and the password hash is never returned by the API.
- **Login** (`POST /api/auth/login`) checks the credentials and returns a signed JWT (HS256, 24 hour expiry). The signing secret comes from the `JWT_SECRET` environment variable (at least 32 characters) and is not committed to the repo.
- **A request filter** (`JwtAuthFilter`) reads the `Authorization: Bearer` header on every request and identifies the user. `SecurityConfig` keeps the API stateless, makes only login and registration public, and returns 401 for missing or invalid tokens.
- **Per-user authorization:** the user is always taken from the token (`CurrentUser`), never trusted from the URL or request body. Asking for someone else's data returns 403 or 404 (404 where confirming the record exists would leak information). This prevents IDOR (insecure direct object reference) attacks, where changing an id in a URL exposes another user's data.
- **AI tools:** `getPolicyDetails`, `getClaimStatus` and `createClaim` only act on the logged-in user's own policies and claims. The model is not a security boundary, so the checks live in the tool code, not in the prompt.
- **Error handling:** `GlobalExceptionHandler` returns the real status code for deliberate 403s instead of turning them into a generic 500.

### How I'd explain this in an interview

"I added JWT authentication to my Spring Boot insurance assistant. Passwords are BCrypt-hashed, a filter validates the token on every request, and every endpoint checks that the data belongs to the logged-in user. I tested for IDOR by trying to read another account's data. The AI tool-calling agent uses the same checks, so the model can't be prompted into reading or filing against someone else's policy."

## Frontend (Phase 3, updated in Phase 8)

### Why vanilla JavaScript

The backend and AI integration are the focus of this project, so the frontend uses plain HTML/CSS/JavaScript rather than a framework. There is nothing here (routing, complex state, reusable components) that justifies React or a build step.

### How it talks to the backend

Every page loads `js/api.js` first. It centralizes the API base URL and a single `apiRequest()` helper that builds URLs, sets headers, attaches the JWT, and turns failed responses into readable errors. The page-specific scripts (`login.js`, `dashboard.js`, `policy.js`, `claims.js`, `assistant.js`) all go through it.

### Login and session

`login.js` calls `POST /api/auth/login`, and `api.js` stores the token and user id in `localStorage`. Every other page redirects to `login.html` if there is no token, and any 401 response clears the session and sends the user back to login. The Register button on the login page calls the real registration API.

### Pages and the APIs they use

| Page | Calls |
|---|---|
| `login.html` | `POST /api/auth/login`, `POST /api/users/register` |
| `dashboard.html` | `GET /api/users/{id}`, `GET /api/policies/user/{userId}`, `GET /api/claims/policy/{policyId}` |
| `policy.html` | `GET /api/policies/user/{userId}`, `GET /api/policies/{id}` |
| `claims.html` | `GET /api/policies/user/{userId}`, `GET /api/claims/policy/{policyId}`, `POST /api/claims` |
| `assistant.html` | `POST /api/ai/chat` |

### CORS

Because the frontend and backend run as separate processes, `WebConfig` allows cross-origin requests from a single configurable origin (`FRONTEND_ORIGIN`, default `http://localhost:5500`, VS Code Live Server's default port) rather than `allowedOrigins("*")`. If you open the frontend at `http://127.0.0.1:5500`, set `FRONTEND_ORIGIN` to match.

### Running the frontend locally

The frontend is static files with no build step. Open the `frontend/` folder with the VS Code "Live Server" extension, or run any static file server from that folder (for example `python3 -m http.server 5500`). Start the backend first.

## LLM integration (Phase 4)

### Provider

Uses **Groq** through Spring AI's OpenAI-compatible client (`spring-ai-starter-model-openai`). Groq's API is OpenAI-compatible, so `application.properties` points the existing OpenAI client at Groq's base URL instead of adding a Groq-specific dependency:

```properties
spring.ai.openai.api-key=${GROQ_API_KEY:}
spring.ai.openai.base-url=https://api.groq.com/openai
spring.ai.openai.chat.options.model=${GROQ_MODEL:openai/gpt-oss-120b}
```

Groq was chosen because it has a genuinely free tier and is a supported, documented way to use Spring AI.

**Note on model choice:** Groq's free-tier lineup changes fairly often. This project originally used `llama-3.3-70b-versatile`, which Groq deprecated for free/developer accounts in June 2026. The failure showed up as an HTTP 404 `model_not_found`, logged by `AiAssistantService` and shown to the user as the safe fallback message. The model was switched to `openai/gpt-oss-120b`. If this happens again, check `console.groq.com/docs/models` for models marked "Production" and update `GROQ_MODEL`; no code change is needed.

### Architecture

```
Frontend (assistant.html)
      ↓
POST /api/ai/chat   (JWT required)
      ↓
AiAssistantController   (HTTP only: validates the request, calls the service)
      ↓
AiAssistantService      (routes the question, builds the prompt, calls the model, handles failures)
      ↓
Spring AI ChatClient
      ↓
Groq (LLM)
```

The API key never reaches the browser. The frontend only calls this app's own `/api/ai/chat` endpoint.

### Error handling

`AiAssistantService.answer()` catches any failure from the provider (missing or invalid API key, Groq being down, a timeout, a rate limit, an empty response, a vector-search or tool-call failure) and returns one safe fallback message: "The AI assistant is temporarily unavailable. Please try again." The real exception is logged server-side (message only, not the user's question, since it could contain sensitive details).

### Input validation

`AiChatRequest` requires a non-blank question, capped at 500 characters.

### How I'd explain this in an interview

"I integrated the LLM through the Spring Boot backend using Spring AI, with Groq as the provider since it's OpenAI-API-compatible and has a free tier. The frontend only talks to my own `/api/ai/chat` endpoint and never sees the API key. Any failure from the AI provider returns one safe fallback message instead of leaking an error, while the real cause is logged on the backend."

## RAG (Phase 5)

### Why a second database

RAG needs to store and search document embeddings (vectors), which MySQL can't do. Rather than replace MySQL, this app uses **two separate databases**: MySQL for application data, and **PostgreSQL with the pgvector extension** only for document embeddings. `DataSourceConfig` defines both `DataSource` beans explicitly: the MySQL one stays `@Primary` (used by JPA), and the Postgres one is used only by the vector store.

### Why local embeddings instead of another API

Groq doesn't offer an embeddings endpoint. Rather than sign up for a second AI provider, this project uses Spring AI's local ONNX embedding model (`spring-ai-starter-model-transformers`, `all-MiniLM-L6-v2`, 384 dimensions). It runs inside the JVM, downloads the model once, and needs no API key.

### Architecture

```
Startup:
  documents/*.txt  →  TextReader  →  TokenTextSplitter  →  local embeddings  →  pgvector

Question:
  User question
        ↓
  AiAssistantService
        ↓
  vectorStore.similaritySearch(question, topK=3)   →  pgvector
        ↓
  top 3 relevant chunks + question  →  prompt  →  Groq (chat)
        ↓
  {answer, sources}
```

`DocumentIngestionService` runs once at startup (`CommandLineRunner`), reads every `.txt` file in `src/main/resources/documents/`, splits each into chunks, tags each chunk with its source filename, then embeds and stores them. It skips re-ingestion if the store is already populated.

### Sample documents

Four small, realistic documents: `health-insurance-policy.txt`, `hospitalization-coverage.txt`, `claim-process.txt` and `exclusions.txt`. They cover coverage, hospitalization, the claims process and exclusions.

### Grounding

The prompt is built from the retrieved chunks, and the system prompt tells the model not to invent policy details, coverage limits or eligibility beyond the given context.

### Sources in the response

```json
{
  "answer": "Hospitalization is covered, subject to the room rent limit...",
  "sources": ["hospitalization-coverage.txt"]
}
```

The AI Assistant page shows the sources under each answer.

### Configuration

```properties
app.vector-datasource.url=jdbc:postgresql://${VECTOR_DB_HOST:localhost}:${VECTOR_DB_PORT:5433}/${VECTOR_DB_NAME:insurance_vectors}
app.vector-datasource.username=${VECTOR_DB_USERNAME:postgres}
app.vector-datasource.password=${VECTOR_DB_PASSWORD:}
```

`VectorStoreConfig` builds the `PgVectorStore` bean directly (`initializeSchema(true)`) instead of using the auto-configuring starter, so it uses the dedicated Postgres `DataSource` and not the MySQL one.

### Running pgvector locally without Docker Compose

```
docker run -d --name pgvector-db -p 5433:5432 -e POSTGRES_PASSWORD=postgres pgvector/pgvector:pg16
```

Port 5433 is used on the host because a locally installed PostgreSQL commonly occupies 5432 (check with `netstat -ano | findstr :5432` on Windows). On first run, look for "Ingested N chunks from 4 policy documents" in the logs.

### How I'd explain this in an interview

"For RAG I added PostgreSQL with the pgvector extension alongside MySQL: MySQL still handles application data, and Postgres only stores document embeddings. I used a local open-source embedding model because Groq doesn't offer embeddings. On startup the app reads the policy documents, chunks and embeds them. When a user asks a question I do a similarity search, pass only the relevant chunks to the LLM, and return the answer with its sources. If nothing relevant is found, it says so instead of guessing."

## Prompt engineering (Phase 6)

`PromptTemplates` holds three system prompts: a general policy prompt, an empathetic claim prompt, and a short out-of-scope prompt that politely redirects. `AiAssistantService` routes each question with simple keyword checks: claim-related words select the claim prompt, and clearly unrelated topics (weather, movies and similar) skip vector search and tools entirely. The checks are deliberately conservative, so an ambiguous question still goes through the normal grounded path.

## AI agent and tool calling (Phase 7)

`InsuranceTools` exposes three `@Tool` methods through Spring AI's `ChatClient.tools(...)`:

| Tool | What it does |
|---|---|
| `getPolicyDetails(policyId)` | Returns plan, coverage, premium, status and dates for one of the user's policies |
| `getClaimStatus(claimNumber)` | Returns the status of one of the user's claims (accepts formats like `CLM1001` and `CLM-2026-4821`) |
| `createClaim(policyId, type, description, amount)` | Files a claim, only when the model has all four values from the customer |

The model decides on its own when a question needs a tool and when it can answer from the document context. Since Phase 8, every tool only sees the logged-in user's own policies and claims; see [Security](#security-phase-8).

## Testing (Phase 9)

Run all tests with:

```
mvn test
```

The tests need no running database or API key. Repositories and the AI model are mocked, so the suite runs in about ten seconds. 41 tests, all passing.

| Area | Test class | What it checks |
|---|---|---|
| DTO validation | `AiChatRequestValidationTest`, `CreateClaimRequestValidationTest` | Bad request bodies are rejected (blank or oversized question, zero or negative claim amount, blank description) |
| Services | `PolicyServiceTest`, `ClaimServiceTest`, `AiAssistantServiceTest` | Business logic, 404 handling, and the AI fallback paths (provider failure, empty response, vector-search failure) with Spring AI's `ChatClient` and `VectorStore` mocked |
| JWT | `JwtServiceTest` | Valid tokens pass; forged, expired and garbage tokens are rejected |
| Passwords | `UserServicePasswordTest` | Passwords are stored as BCrypt hashes; duplicate emails are refused and nothing is saved |
| AI tools | `InsuranceToolsTest` | The tools only see the logged-in user's policies and claims; a claim on someone else's policy is refused and never saved |
| HTTP security | `SecurityEndpointsTest` | `@WebMvcTest` + `MockMvc` through the real filter chain: correct login returns a token; wrong password, unknown email, missing token or a bad token gives 401; another user's data gives 403 or 404 |

Not covered yet: end-to-end tests against real MySQL/pgvector (for example with Testcontainers) and calls to the live LLM.

## Docker and CI/CD (Phases 10 and 11)

- **`Dockerfile`**: a two-stage build. Stage one compiles the jar with Maven and a JDK; stage two copies only the jar into a small Java 17 runtime image.
- **`docker-compose.yml`**: runs the backend, MySQL and pgvector together. Health checks make the backend wait until both databases are ready, containers reach each other by service name (`mysql`, `pgvector`), data lives in named volumes, and secrets come from `.env` rather than being baked into the image.
- **`.dockerignore`** keeps `.env`, `target/` and `.git` out of the image.
- **`.github/workflows/ci.yml`**: on every push and pull request to `main`, GitHub Actions runs `mvn test`, and only if that passes does it build the Docker image. The badge at the top of this README shows the status.

### How I'd explain this in an interview

"I containerised the app with a multi-stage Dockerfile and a Compose file that runs the Spring Boot backend, MySQL and a pgvector Postgres together, with health checks so the app only starts when both databases are ready and secrets passed through environment variables. GitHub Actions runs the test suite on a clean machine on every push, and only builds the Docker image if the tests pass."

## Known limitations and future improvements

- The JWT is stored in the browser's `localStorage`; httpOnly cookies would be safer in production.
- No refresh tokens, password reset, or rate limiting on login yet.
- Demo seed users use a known password and are for local development only.
- The frontend is not containerized; it is served separately as static files.
- Monitoring (Spring Boot Actuator health endpoint) and a Postman collection are planned.
- Integration tests against real databases (Testcontainers) and automated tests against the live LLM are not included.