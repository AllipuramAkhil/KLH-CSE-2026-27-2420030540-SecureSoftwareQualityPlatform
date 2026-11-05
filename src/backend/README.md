# AI-Driven Adaptive Software Quality Assurance and Security Automation Platform (ASSQP)

## Overview

ASSQP is a unified backend platform that brings together **Software Quality Assurance (SQA)**, **cybersecurity**, **adaptive testing**, **risk-based prioritization**, **AI-assisted analysis**, and **continuous feedback** into a single Spring Boot service, designed to plug into a **CI/CD pipeline**.

Instead of relying on static, manually maintained test suites and disconnected quality/security tooling, ASSQP:

- Analyzes uploaded source code for **quality metrics** (complexity, duplication, maintainability, code smells) using a self-contained rule-based analyzer.
- Scans the same source code for **security vulnerabilities** (SQL injection, XSS, command injection, hardcoded secrets, weak crypto, insecure configuration, and more) using a custom rule-based scanner.
- Computes **component-level risk** as `Risk = Probability × Impact`, combining vulnerability severity, defect history, and test coverage.
- Runs an **Adaptive Testing Engine** that dynamically reorders test execution based on historical failure rate, recent code changes, component risk, and related vulnerability severity — the platform's core differentiator.
- Optionally enriches all of the above with an **AI provider** for natural-language explanations and test generation — but **never depends on it**. Every AI-driven feature has a deterministic rule-based fallback, so the platform is fully functional with zero AI configuration.
- Records every analysis cycle into history (`AnalysisHistory`) so each new cycle's decisions are informed by the last — a genuine continuous feedback loop, not a one-shot pipeline.
- Exposes a **CI/CD webhook** that triggers quality, security, and risk analysis automatically on every build.

## Features

| Area | What it does |
|---|---|
| **Authentication** | JWT access + refresh tokens, BCrypt password hashing, 4 roles (`ADMIN`, `DEVELOPER`, `TESTER`, `SECURITY_ANALYST`) |
| **Role-based access** | Method-level `@PreAuthorize` on admin-only operations; project-level ownership checks |
| **Project management** | Full CRUD, paginated listing, per-project quality/security/risk scores |
| **Source code upload** | Multipart upload with type/size/path-traversal validation, metadata persisted for later analysis |
| **Quality analysis** | LOC, method count, cyclomatic complexity, duplication estimate, maintainability index, code smells, potential bugs, technical debt, 0–100 quality score |
| **Security analysis** | Pattern-based detection across 8 vulnerability categories, CWE/OWASP mapping, severity classification, status workflow |
| **Test management** | Full CRUD for test cases, filterable by type/priority/status, execution history tracking |
| **Test execution** | Records pass/fail/skip/block/error outcomes, automatically updates each test case's rolling historical failure rate |
| **Adaptive testing** | Computes a normalized 0.0–1.0 priority score per test case and returns a recommended execution order with human-readable reasoning |
| **Risk assessment** | Component-level `Risk = Probability × Impact`, risk-level classification, project risk summary |
| **AI analysis** | Test generation, defect explanation, vulnerability explanation, risk explanation — all AI-assisted with automatic rule-based fallback |
| **AI fallback** | If `AI_API_KEY`/`AI_API_URL` are unset or the provider call fails, every AI endpoint transparently falls back to deterministic rule-based output instead of failing |
| **CI/CD integration** | Webhook endpoint + manual trigger; both run quality, security, and risk analysis automatically and record a pipeline run |
| **Dashboard** | Single endpoint aggregating test pass/fail counts, coverage, vulnerability counts by severity, scores, risk distribution, and execution trends |
| **Reports** | On-demand JSON reports combining quality, security, risk, and testing data; designed so a PDF/CSV exporter can be added later without a schema change |
| **Notifications** | Paginated, unread filtering, mark-read / mark-all-read, user-scoped |

## Technology Stack

- Java 21
- Spring Boot 3.3 (Spring Web, Spring Data JPA, Spring Security, Spring WebFlux for the AI client)
- JWT (`io.jsonwebtoken` / jjwt 0.12.6)
- MySQL (`mysql-connector-j`)
- Lombok
- Jakarta Bean Validation
- SpringDoc OpenAPI (Swagger UI)
- Spring Boot Actuator
- JUnit 5 + Mockito + AssertJ
- H2 (in-memory, test scope only)
- Maven

## Architecture

```
React / Vite Frontend
        │
        ▼
   REST API (JWT-secured)
        │
        ▼
Spring Boot Controllers  (16 controllers, thin — no business logic)
        │
        ▼
   Service Layer          (15 service interfaces + implementations)
        │
        ▼
Analysis / AI / Adaptive Engines
  ├─ RuleBasedQualityAnalyzer      (quality metrics)
  ├─ RuleBasedSecurityScanner      (vulnerability detection)
  ├─ AdaptiveTestingEngine         (TestPriorityCalculator + RiskBasedPrioritizer + TestSelectionEngine)
  ├─ FeedbackProcessor             (writes AnalysisHistory for the next cycle)
  └─ AI engines                    (TestCaseGenerator, DefectPredictionEngine,
                                     VulnerabilityClassificationEngine, RiskPredictionEngine)
                                     → AiClient → external AI provider, or
                                     → rule-based fallback if unavailable
        │
        ▼
   Repositories (Spring Data JPA)
        │
        ▼
      MySQL
```

### AI architecture (explicit fallback path)

```
                ┌───────────────┐
                │   AI Service  │   (configured via AI_API_KEY / AI_API_URL)
                └───────┬───────┘
                        │ optional enhancement
┌───────────────────────▼────────────────────────┐
│             ASSQP Core Engine                   │
│                                                  │
│  Quality Analysis   (always rule-based)          │
│  Security Analysis  (always rule-based)          │
│  Risk Assessment    (always rule-based)          │
│  Adaptive Testing   (always rule-based)          │
│  Test Execution     (always rule-based)          │
│  Historical Feedback (always rule-based)          │
└──────────────────────────────────────────────────┘
```

The four AI-enhanced explanation/generation features (test generation, defect explanation, vulnerability explanation, risk explanation) call `AiClient.isAvailable()` first. If the provider isn't configured, or the call throws `AiServiceException`, each engine falls back to its own deterministic rule-based method — the response shape (`AiAnalysisResponse`) is identical either way, with an `aiGenerated` / `source` field indicating which path was used.

### Adaptive testing workflow

```
Code Changes
      │
      ▼
Quality Analysis  ──────┐
      │                 │
      ▼                 │
Security Analysis  ─────┤
      │                 │
      ▼                 │
Risk Assessment  ───────┤──► AnalysisHistory (FeedbackProcessor)
      │                 │
      ▼                 │
Adaptive Test Prioritization  (reads history + current risk + vulnerabilities)
      │
      ▼
Test Execution
      │
      ▼
Results  →  Historical failure rate updated per test case
      │
      ▼
Feedback  (POST /adaptive-testing/feedback)
      │
      ▼
Next Testing Cycle
```

`TestPriorityCalculator` combines, per test case:

- Historical failure rate (25%)
- Most recent execution result (20%)
- Related component's current risk score (20%)
- Highest-severity vulnerability tied to the same component (15%)
- Whether the test case was recently modified (12%)
- Staleness — days since last execution (8%)

...into a single 0.0–1.0 score, then classifies it as `LOW` / `MEDIUM` / `HIGH` / `CRITICAL`.

### Security analysis approach

`RuleBasedSecurityScanner` is a self-contained, dependency-free pattern scanner covering:

| Category | CWE | OWASP |
|---|---|---|
| SQL Injection | CWE-89 | A03:2021 |
| Cross-Site Scripting | CWE-79 | A03:2021 |
| Command Injection | CWE-78 | A03:2021 |
| Hardcoded Secrets | CWE-798 | A07:2021 |
| Broken Access Control | CWE-284 | A01:2021 |
| Weak Cryptography | CWE-327 | A02:2021 |
| Insecure Configuration | CWE-16 | A05:2021 |
| Sensitive Data Exposure | CWE-200 | A02:2021 |

It is intentionally decoupled behind a `SecurityScanner` interface so a future integration with Semgrep, OWASP Dependency-Check, or Trivy can be dropped in without touching the service layer.

## Project Structure

```
assqp-backend/
├── pom.xml
├── README.md
├── .gitignore
├── .env.example
│
└── src/
    ├── main/
    │   ├── java/com/assqp/platform/
    │   │   ├── AssqpApplication.java
    │   │   ├── config/          (SecurityConfig, CorsConfig, OpenApiConfig, JacksonConfig, AsyncConfig, WebClientConfig)
    │   │   ├── controller/      (16 REST controllers)
    │   │   ├── dto/             (auth, user, project, analysis, testing, risk, ai, cicd, dashboard, notification)
    │   │   ├── entity/          (13 JPA entities)
    │   │   ├── repository/      (13 Spring Data JPA repositories)
    │   │   ├── service/         (15 service interfaces)
    │   │   ├── service/impl/    (15 service implementations)
    │   │   ├── security/        (JWT filter, UserPrincipal, JwtService, CustomUserDetailsService)
    │   │   ├── analysis/        (RuleBasedQualityAnalyzer, RuleBasedSecurityScanner + interfaces)
    │   │   ├── adaptive/        (AdaptiveTestingEngine, TestPriorityCalculator, RiskBasedPrioritizer,
    │   │   │                     TestSelectionEngine, FeedbackProcessor)
    │   │   ├── ai/               (AiClient + impl, AiPromptService, TestCaseGenerator,
    │   │   │                     DefectPredictionEngine, VulnerabilityClassificationEngine, RiskPredictionEngine)
    │   │   ├── exception/        (custom exceptions + GlobalExceptionHandler)
    │   │   ├── enums/            (Role, ProjectStatus, Severity, VulnerabilityStatus, TestType,
    │   │   │                     TestCaseStatus, ExecutionStatus, RiskLevel, NotificationType, PipelineStatus)
    │   │   └── util/             (FileValidationUtil, ScoreUtil)
    │   └── resources/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       └── application-prod.properties
    │
    └── test/
        ├── java/com/assqp/platform/
        │   ├── AssqpApplicationTests.java
        │   ├── adaptive/      (AdaptiveTestingEngineTest, TestPriorityCalculatorTest)
        │   ├── ai/            (TestCaseGeneratorTest — AI fallback coverage)
        │   ├── analysis/      (RuleBasedQualityAnalyzerTest, RuleBasedSecurityScannerTest)
        │   ├── exception/     (GlobalExceptionHandlerTest)
        │   └── service/       (Auth, Project, SecurityAnalysis, TestCase, TestExecution,
        │                       RiskAssessment, Report, CiCd, Notification)
        └── resources/
            └── application-test.properties   (H2 in-memory config)
```

## Database Setup

```sql
CREATE DATABASE assqp;
```

The schema is created/updated automatically via `spring.jpa.hibernate.ddl-auto=update` in development. Use `validate` in production (see `application-prod.properties`) once the schema is finalized.

## Environment Variables

Copy the template and fill in real values:

```bash
cp .env.example .env
```

| Variable | Purpose | Required |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection | Yes |
| `JWT_SECRET` | HMAC-SHA256 signing key (256+ bits) | Yes |
| `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION` | Token lifetimes (ms) | No (defaults provided) |
| `AI_API_KEY`, `AI_API_URL`, `AI_API_MODEL` | External AI provider | No — platform runs fully on rule-based fallback without these |
| `UPLOAD_DIR` | Local storage path for uploaded source files | No |

Spring Boot reads these via `${VARIABLE:default}` placeholders in `application.properties` — no secret is ever hardcoded in source.

## Running the Project

```bash
git clone <repository>
cd assqp-backend

# create the database
mysql -u root -p -e "CREATE DATABASE assqp;"

# configure environment
cp .env.example .env
# edit .env with real values

mvn clean install
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

## Swagger / OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

JWT auth is wired into Swagger UI — click **Authorize** and paste `Bearer <access_token>` after logging in.

## Actuator

- Health: `http://localhost:8080/actuator/health`

Only `health` and `info` are exposed (`management.endpoints.web.exposure.include=health,info`); no environment, beans, or config-props endpoints are exposed, so no credentials or internal state can leak through Actuator.

## Authentication Flow

1. `POST /api/auth/register` → creates a user, returns access + refresh token.
2. `POST /api/auth/login` → returns access + refresh token for existing credentials.
3. Send `Authorization: Bearer <accessToken>` on every subsequent request.
4. `POST /api/auth/refresh` with the refresh token → returns a new token pair once the access token expires.
5. `GET /api/auth/me` → returns the authenticated user's profile.

### Sample: Register

```http
POST /api/auth/register
Content-Type: application/json

{
  "fullName": "Akhil Allipuram",
  "email": "akhil@example.com",
  "password": "SecurePass123",
  "role": "DEVELOPER"
}
```

### Sample: Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "akhil@example.com",
  "password": "SecurePass123"
}
```

Response:

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "userId": 1,
  "fullName": "Akhil Allipuram",
  "email": "akhil@example.com",
  "role": "DEVELOPER"
}
```

### Sample: Create a project

```http
POST /api/projects
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "name": "ASSQP Capstone",
  "description": "Adaptive QA and security automation platform",
  "repositoryUrl": "https://github.com/akhil/assqp-backend",
  "branch": "main",
  "technologyStack": "Java 21, Spring Boot, MySQL"
}
```

### Sample: Run quality analysis

```http
POST /api/projects/1/quality/analyze
Authorization: Bearer <accessToken>
```

### Sample: Run security analysis

```http
POST /api/projects/1/security/analyze
Authorization: Bearer <accessToken>
```

### Sample: Adaptive test prioritization

```http
POST /api/projects/1/adaptive-testing/prioritize
Authorization: Bearer <accessToken>
```

Response:

```json
{
  "projectId": 1,
  "totalTests": 2,
  "recommendedTests": [
    {
      "testCaseId": 15,
      "title": "Payment flow test",
      "priorityScore": 0.94,
      "riskLevel": "CRITICAL",
      "recommendedOrder": 1,
      "reason": "High historical failure rate; recent code changes in related component; associated with CRITICAL severity vulnerability; "
    },
    {
      "testCaseId": 8,
      "title": "Stable getter test",
      "priorityScore": 0.12,
      "riskLevel": "LOW",
      "recommendedOrder": 2,
      "reason": "Standard regression priority based on baseline coverage and stability."
    }
  ]
}
```

### Sample: Dashboard

```http
GET /api/projects/1/dashboard
Authorization: Bearer <accessToken>
```

```json
{
  "projectId": 1,
  "totalTests": 25,
  "passedTests": 20,
  "failedTests": 3,
  "testCoverage": 88.0,
  "totalVulnerabilities": 6,
  "criticalVulnerabilities": 1,
  "highVulnerabilities": 2,
  "openDefects": 3,
  "qualityScore": 84.5,
  "securityScore": 78.0,
  "riskScore": 32.5,
  "recentAnalyses": ["QUALITY_ANALYSIS: Quality analysis completed with score 84.5"],
  "riskDistribution": {"LOW": 4, "MEDIUM": 2, "HIGH": 1, "CRITICAL": 0},
  "testExecutionTrends": [{"date": "2026-08-14", "executions": 12}],
  "mostRiskyComponents": ["PaymentController", "AuthService"]
}
```

## Testing

The test suite uses JUnit 5, Mockito, and AssertJ (all provided transitively by `spring-boot-starter-test`), plus H2 for the Spring context integration test.

```bash
mvn test
```

Coverage includes:

- **Authentication**: registration (success + duplicate email), login (success + bad credentials)
- **Authorization**: project ownership enforcement (forbidden access for non-owners), admin bypass
- **Project management**: create, get (found/not found), paginated listing scoped to owner, delete
- **Quality analysis**: rule-based analyzer on clean vs. complex/smelly code, empty-file edge case
- **Security analysis**: hardcoded-secret detection, SQL injection detection, clean-code negative case, status transitions, invalid status rejection
- **Test case management**: create, invalid test type rejection, status update, type-based filtering, delete
- **Test execution**: execution recording, historical failure rate recalculation (pass vs. fail), execution history retrieval
- **Adaptive testing**: deterministic priority ordering (same inputs → same order across repeated runs), high-risk test ranked above low-risk test
- **Risk assessment**: `Risk = Probability × Impact` for a critical/untested component vs. a low-severity/well-tested component
- **AI fallback**: AI available → AI response used; AI unavailable → rule-based fallback; AI throws → graceful fallback (no exception propagates)
- **Reports**: generate, retrieve, not-found, delete
- **CI/CD**: webhook success path, webhook failure path (pipeline marked `FAILED`, not thrown), manual trigger
- **Notifications**: pagination, unread-only filtering, mark-read (with ownership check), mark-all-read, delete
- **Global exception handling**: 404 structure, 400 structure with field errors, 503 for AI unavailability (not 500), and confirmation that internal exception messages never leak to the client

**⚠️ Maven build verification could not be executed in this environment because Maven Central dependency resolution is blocked by the egress proxy with HTTP 403.** All source, test, and configuration files have instead been verified through exhaustive static analysis: every controller→service method call, every repository method call, every internal import across all 149+ source files, every DTO field access, every enum reference, and every route mapping were checked against their actual declarations — with zero mismatches found. This is not a substitute for `mvn clean test`; please run that command in an environment with Maven Central access before deploying.

## CI/CD Integration

`POST /api/cicd/webhook` accepts pipeline metadata from any CI system:

```json
{
  "projectId": 1,
  "branch": "main",
  "commitHash": "abc123",
  "buildNumber": "142",
  "triggerSource": "github-actions"
}
```

On receipt, the platform automatically runs quality analysis → security analysis → risk assessment against the project's currently uploaded source files, and records the pipeline's success/failure status. `POST /api/projects/{projectId}/cicd/trigger` performs the same flow on demand, without requiring a webhook.

## API Endpoints

### Authentication
```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh
GET    /api/auth/me
```

### Users
```
GET    /api/users                 (ADMIN)
GET    /api/users/{id}            (ADMIN)
PUT    /api/users/{id}            (ADMIN)
DELETE /api/users/{id}            (ADMIN)
GET    /api/users/me
PUT    /api/users/me
```

### Projects
```
POST   /api/projects
GET    /api/projects              (paginated: ?page=&size=&sort=)
GET    /api/projects/{id}
PUT    /api/projects/{id}
DELETE /api/projects/{id}
```

### Source Code
```
POST   /api/projects/{projectId}/source/upload   (multipart)
GET    /api/projects/{projectId}/source
GET    /api/source/{id}
DELETE /api/source/{id}
```

### Quality
```
POST   /api/projects/{projectId}/quality/analyze
GET    /api/projects/{projectId}/quality
GET    /api/projects/{projectId}/quality/latest
GET    /api/projects/{projectId}/quality/{analysisId}
```

### Security
```
POST   /api/projects/{projectId}/security/analyze
GET    /api/projects/{projectId}/security
GET    /api/projects/{projectId}/security/latest
GET    /api/projects/{projectId}/security/vulnerabilities   (?severity=&status=)
GET    /api/vulnerabilities/{id}
PATCH  /api/vulnerabilities/{id}/status
```

### Testing
```
POST   /api/projects/{projectId}/tests
GET    /api/projects/{projectId}/tests   (?type=&priority=&status=, paginated)
GET    /api/tests/{id}
PUT    /api/tests/{id}
PATCH  /api/tests/{id}/status
DELETE /api/tests/{id}
POST   /api/tests/{testId}/execute
GET    /api/tests/{testId}/executions
GET    /api/executions/{id}
GET    /api/projects/{projectId}/executions
```

### Adaptive Testing
```
POST   /api/projects/{projectId}/adaptive-testing/prioritize
GET    /api/projects/{projectId}/adaptive-testing/recommendations
POST   /api/projects/{projectId}/adaptive-testing/feedback
GET    /api/projects/{projectId}/adaptive-testing/history
```

### Risk
```
POST   /api/projects/{projectId}/risk/analyze
GET    /api/projects/{projectId}/risk
GET    /api/projects/{projectId}/risk/{id}
GET    /api/projects/{projectId}/risk/summary
```

### AI
```
POST   /api/projects/{projectId}/ai/analyze
POST   /api/projects/{projectId}/ai/generate-tests
POST   /api/projects/{projectId}/ai/recommendations
POST   /api/projects/{projectId}/ai/explain-defect
POST   /api/projects/{projectId}/ai/explain-vulnerability
```

### Dashboard
```
GET    /api/projects/{projectId}/dashboard
```

### Reports
```
POST   /api/projects/{projectId}/reports
GET    /api/projects/{projectId}/reports
GET    /api/reports/{id}
DELETE /api/reports/{id}
```

### CI/CD
```
POST   /api/cicd/webhook
GET    /api/cicd/{id}
GET    /api/projects/{projectId}/cicd
POST   /api/projects/{projectId}/cicd/trigger
```

### Notifications
```
GET    /api/notifications          (paginated)
GET    /api/notifications/unread
PATCH  /api/notifications/{id}/read
PATCH  /api/notifications/read-all
DELETE /api/notifications/{id}
```

### Health
```
GET    /api/health
```

## License

Academic capstone project — for coursework use.
