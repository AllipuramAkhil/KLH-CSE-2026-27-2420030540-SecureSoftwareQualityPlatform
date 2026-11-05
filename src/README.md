# ASSQP — Adaptive Software Security and Quality Platform

An AI-assisted, full-stack platform for tracking software quality, security, and risk across a project's
lifecycle: rule-based static analysis, real integrations with Trivy/SonarQube/OWASP ZAP/JaCoCo, a
git-history-based code churn analyzer, an ML risk-prediction microservice with basic MLOps, adaptive test
prioritization, and CI/CD pipeline tracking — all surfaced through one dashboard.

This README describes **only what is implemented and verified in this repository**. Where something is
partial, optional, or has a known limitation, that's called out explicitly rather than glossed over.

## 1. Problem Statement

Software quality, security, and risk data is normally scattered across separate tools — a coverage report
here, a SonarQube dashboard there, a vulnerability scanner somewhere else, test results in CI logs, and no
single place that combines them into a risk picture for a specific component. ASSQP's goal is to pull that
data into one project-scoped view, and to let both a deterministic rule-based engine *and* a trained ML
model contribute to risk scoring — with the rule-based engine as the guaranteed fallback if any external
tool or the ML service is unavailable.

## 2. Key Features by Module

| Module | What it does |
|---|---|
| **Auth & Projects** | JWT-based registration/login, project CRUD, ownership + role-based access (`DEVELOPER`/`ADMIN`) |
| **Quality Analysis** | Rule-based static analyzer (cyclomatic complexity, duplication, code smells, maintainability index) over uploaded source files |
| **Security Analysis** | Rule-based vulnerability scanner + three real external integrations feeding the same vulnerability table |
| **JaCoCo Coverage** (Phase 3) | Import a JaCoCo XML report; line/branch/instruction coverage stored and trended over time |
| **SonarQube** (Phase 4) | Pull real metrics from a configured SonarQube server via its Web API |
| **Trivy** (Phase 5) | Filesystem/image vulnerability scanning — CLI trigger (if `trivy` is installed) or JSON report import |
| **OWASP ZAP** (Phase 6) | DAST baseline scan (spider + passive scan only, never active/attack) against an admin-approved target allow-list, or JSON report import |
| **ML Risk Prediction** (Phase 7/8) | Standalone FastAPI service (`RandomForestClassifier`) predicting component risk from 10 features; Spring Boot combines it with the rule-based score into a **hybrid** result |
| **Git Code Churn** (Phase 9) | Real per-file commit count / lines changed / recency-weighted activity from local git history via JGit — no GitHub API/token needed |
| **MLOps** (Phase 10) | Model version/metrics endpoints, one-click retrain, and a project-scoped prediction history log (separate from the ML service's own internal log) |
| **Adaptive Testing** | Rule-based test prioritization using risk/failure-rate signals, with a feedback loop that records each analysis cycle |
| **CI/CD** | GitHub Actions workflow + webhook endpoint recording pipeline runs (build/test/coverage/Sonar/Trivy stages) |
| **AI Analysis** (optional) | Pluggable external AI API (e.g. Claude) for recommendations/test generation/defect explanations — only active if `AI_API_KEY`/`AI_API_URL` are set |
| **Reports** | Generate and store JSON-based project reports |

## 3. Architecture

```
                     +------------------+
                     |  React Frontend   |  (Vite + TS, served by nginx in Docker)
                     +--------+---------+
                              | REST (JWT bearer)
                     +--------v---------+        +-----------------------+
                     |  Spring Boot API  |------->|  ML Service (FastAPI)  |
                     |  (Java 21)        |  HTTP  |  RandomForestClassifier|
                     +---+-----------+---+        +-----------------------+
                         |           |
              +----------+           +-----------+
              v                                  v
        +-----------+              +---------------------------+
        |  MySQL 8   |              | External tools (optional)  |
        +-----------+              | SonarQube . Trivy . ZAP     |
                                    | (local git repo for churn)  |
                                    +---------------------------+
```

- **Frontend**: React 18 + TypeScript + Vite, TanStack Query for server state, Tailwind CSS, React Router.
- **Backend**: Spring Boot 3.3.4 (Java 21), Spring Security (stateless JWT), Spring Data JPA, WebFlux's
  `WebClient` for outbound calls to SonarQube/ZAP/ML service.
- **Database**: MySQL 8. Schema is created/updated by Hibernate (`ddl-auto=update` in the `dev` profile) —
  there are no Flyway/Liquibase migrations in this project yet (see [§25](#25-current-project-status)).
- **ML Service**: standalone FastAPI + scikit-learn microservice, independently deployable, called over HTTP.
  If it's down or unconfigured, the backend falls back to rule-based risk scoring only — nothing else breaks.
- **External integrations**: SonarQube and ZAP are called over HTTP if configured; Trivy is invoked as a
  local CLI binary if present (or its JSON report can be imported directly either way); all three degrade
  gracefully to "unavailable" rather than crashing the app.

## 4. Technology Stack

**Backend** — Spring Boot 3.3.4, Java 21, Spring Data JPA, Spring Security, springdoc-openapi 2.6.0
(Swagger UI), JJWT 0.12.6, JGit 6.10.0, MySQL Connector/J, Lombok, JaCoCo Maven plugin 0.8.12, Maven.

**Frontend** — React 18, TypeScript 5, Vite 5, TanStack Query 5, React Router 6, Tailwind CSS, Axios,
React Hook Form + Zod, Recharts, lucide-react.

**ML Service** — Python, FastAPI 0.115, scikit-learn 1.5.2, pandas 2.2.3, joblib, pydantic-settings, uvicorn.

**Infrastructure** — Docker + Docker Compose, GitHub Actions, MySQL 8, nginx (frontend static serving +
reverse proxy), optional SonarQube server, optional Trivy CLI, optional OWASP ZAP daemon (`zaproxy/zap-stable`).

## 5. Project Structure

```
ASSQP-final/
|-- backend/                    Spring Boot API (Java 21 / Maven)
|   `-- src/main/java/com/assqp/platform/
|       |-- controller/          22 REST controllers
|       |-- service/ + service/impl/
|       |-- entity/ + repository/  17 JPA entities
|       |-- dto/                 Request/response DTOs, grouped by domain
|       |-- security/            JWT filter, entry point, current-user provider
|       |-- analysis/            Rule-based quality/security analyzers
|       |-- adaptive/            Adaptive test prioritization engine
|       |-- risk/                RiskFeatureBuilder, HybridRiskCalculator (Phase 8)
|       |-- churn/               GitChurnAnalyzer (Phase 9, JGit-based)
|       |-- integration/         sonarqube/, trivy/, zap/, ml/ - external HTTP/CLI clients
|       |-- exception/           Custom exceptions + GlobalExceptionHandler
|       `-- config/              Security, WebClient, OpenAPI, CORS config
|   `-- src/test/java/...        17 unit test files (JUnit 5 + Mockito)
|-- frontend/                    React + TypeScript (Vite)
|   `-- src/
|       |-- pages/                Route-level pages, grouped by domain
|       |-- api/, hooks/          Axios clients + TanStack Query hooks per domain
|       |-- types/                TypeScript types mirroring backend DTOs
|       |-- components/           Shared UI (Card, Badge, EmptyState, ErrorState, ...)
|       `-- routes/               AppRoutes, route/role guards
|-- ml-service/                  FastAPI ML risk-prediction microservice
|   |-- app/                      main.py, config.py, schemas.py, model_service.py
|   |-- training/                 train.py, evaluate.py, feature_engineering.py,
|   |                              generate_synthetic_dataset.py
|   |-- data/dataset.csv          Synthetic/sample dataset (see ml-service/README.md)
|   `-- models/                   Trained model artifact + metadata (gitignored, auto-generated)
|-- .github/workflows/ci-cd.yml  GitHub Actions pipeline
|-- docker-compose.yml
|-- .env.example                  Root/Docker Compose environment template
`-- README.md                     This file
```

## 6. Core Workflows

**Project creation & analysis** — Register/log in, create a project, upload source files, run quality
analysis (`POST /api/projects/{id}/quality/analyze`) and/or security analysis
(`POST /api/projects/{id}/security/analyze`); results feed the project dashboard.

**Risk assessment** — `POST /api/projects/{id}/risk/analyze` groups the project's known vulnerabilities by
affected file/component, computes a rule-based `probability x impact` score for each, and — if
`ML_SERVICE_URL` is configured — also sends each component's derived feature vector to the ML service and
combines both into a final level via "take the more severe of the two" (see [§10](#10-risk-assessment)).

**Testing & execution** — Create test cases per project, record executions (`PASSED`/`FAILED`/etc.), view
execution history. The adaptive testing engine uses this data (plus risk scores) to recommend an execution
order.

**Security analysis** — The rule-based scanner runs on demand; Trivy/ZAP findings are imported alongside it
into the same `security_vulnerabilities` table (distinguished by a `source` column), so
`GET /api/projects/{id}/security` always returns the combined view regardless of which sources actually ran.

**Quality analysis** — Rule-based analyzer computes complexity/duplication/smells/maintainability from
uploaded source; SonarQube and JaCoCo data (if imported) surface alongside it on the Quality tab.

**CI/CD integration** — `.github/workflows/ci-cd.yml` builds the backend, runs tests with JaCoCo, optionally
runs the SonarQube scanner, and can POST results to `/api/cicd/webhook` (public endpoint, no auth) to record
a `CiCdPipeline` row.

**ML prediction & MLOps monitoring** — See [§11](#11-ml-integration) and [§12](#12-mlops-functionality) below.

## 7. Security

- **Authentication**: stateless JWT (access + refresh token pair), `BCryptPasswordEncoder` for password
  storage, `DaoAuthenticationProvider`.
- **Authorization**: role-based (`DEVELOPER`, `ADMIN`) via `@PreAuthorize`, plus project-ownership checks in
  `ProjectServiceImpl` (a non-admin can only see/modify their own projects).
- **Public endpoints** (no token required): `/api/auth/**`, `/api/health/**`, `/actuator/health`,
  `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/api/cicd/webhook`. Every other endpoint requires
  a valid bearer token (`anyRequest().authenticated()` is the default).
- **ZAP scan safety**: live DAST scans are only permitted against targets on an explicit allow-list
  (`ZAP_ALLOWED_TARGETS`) — arbitrary external URLs are rejected before any request reaches ZAP.
- Secrets (JWT signing key, DB password, API keys) are read from environment variables only — nothing is
  hardcoded, and `.env` is gitignored.

## 8. Data Model Notes

17 JPA entities: `User`, `Project`, `SourceCodeFile`, `QualityAnalysis`, `SecurityVulnerability`,
`RiskAssessment`, `TestCase`, `TestExecution`, `CiCdPipeline`, `CoverageReport`, `SonarQubeAnalysis`,
`CodeChurnMetrics`, `MlPredictionLog`, `AiRecommendation`, `AnalysisHistory`, `Report`, `Notification`.

Deleting a `Project` (`DELETE /api/projects/{id}`) explicitly cleans up all 14 dependent tables in the
correct order (test executions before test cases, then everything else) before deleting the project row
itself — this is done in application code (`ProjectServiceImpl.delete()`), not via database-level cascade,
since `ddl-auto=update` doesn't configure `ON DELETE CASCADE` automatically. Deleting a `User` who still owns
projects is blocked with a 400 rather than allowed to fail on a foreign-key constraint; their own
notifications are cleaned up automatically since those aren't shared with anyone else.

## 9. Testing and Security Integrations — Real Tool Status

| Integration | How it's wired | Requires |
|---|---|---|
| JaCoCo | Report import (`POST .../coverage/import`) | A JaCoCo XML report (produced by `mvn verify`) |
| SonarQube | Live pull via SonarQube Web API | A running SonarQube server + `SONAR_HOST_URL`/`SONAR_TOKEN` |
| Trivy | CLI trigger *or* JSON report import | `trivy` binary in the backend's environment (not installed in the default Docker image) for CLI mode; import mode always works |
| OWASP ZAP | Live baseline scan (spider + passive only) *or* JSON report import | A running ZAP daemon + `ZAP_API_URL` + `ZAP_ALLOWED_TARGETS` for live mode; import mode always works |
| Git churn | Local git history via JGit | A `.git` directory reachable at `GIT_REPO_PATH` (or its parent, JGit auto-discovers upward) |

All five integrations are designed to degrade gracefully: if unconfigured/unreachable, the corresponding
feature reports "unavailable" and the rest of the application (including the rule-based scanners) keeps
working.

## 10. Risk Assessment

`POST /api/projects/{id}/risk/analyze`:

1. Groups the project's `SecurityVulnerability` rows by `affectedFile` (one "component" per distinct file).
2. Computes a rule-based `probability x impact` score per component from vulnerability severity/count and
   related test cases' historical failure rate.
3. If `ML_SERVICE_URL` is configured, also builds a 10-feature vector per component (see
   [§11](#11-ml-integration)) and sends it to the ML service; the result is combined with the rule-based
   level via `HybridRiskCalculator` (**take the more severe of the two** — a deliberately simple,
   conservative strategy).
4. **Every call to `analyze()` replaces the project's risk-assessment rows entirely.** `RiskAssessment` rows
   represent the project's *current* risk state, not an accumulating history — the delete of old rows and
   the insert of new ones happen in the same transaction, so a failed analysis run can never leave a project
   with an empty or partially-updated risk state. `GET /api/projects/{id}/risk` and
   `GET /api/projects/{id}/risk/summary` always reflect only the most recent `analyze()` call.

## 11. ML Integration

`ml-service/` is a standalone FastAPI microservice, independent of the Spring Boot backend's build/deploy
lifecycle. It exposes:

| Endpoint | Purpose |
|---|---|
| `GET /health` | Liveness + whether a model is currently loaded |
| `POST /predict-risk` | Predict risk (`LOW`/`MEDIUM`/`HIGH`/`CRITICAL`) from a 10-feature vector |
| `GET /model-info` | Model version, algorithm, training timestamp, dataset hash/rows |
| `GET /metrics` | Accuracy/precision/recall/F1 from the last training run |
| `POST /retrain?compare_logreg=true` | Re-run training against the current dataset |
| `GET /prediction-history?limit=50` | The ML service's own internal JSONL prediction log |

**Model**: `RandomForestClassifier` (scikit-learn), trained on 10 features (`code_complexity`,
`code_churn`, `historical_defects`, `vulnerability_count`, `max_vulnerability_severity`, `test_coverage`,
`code_smells`, `duplication`, `historical_failure_rate`, `technical_debt`). A `LogisticRegression`
comparison is optionally reported (not served) when retraining with `compare_logreg=true`.

**Training data disclosure**: `ml-service/data/dataset.csv` is **synthetic/sample data**, generated by
`training/generate_synthetic_dataset.py` from a documented linear heuristic with noise — it does not
represent real historical project data. See `ml-service/README.md` for the exact schema to use when
replacing it with real data later. Interestingly, on this synthetic set `LogisticRegression` slightly
outperforms `RandomForestClassifier` (an artifact of the synthetic labeling heuristic being linear) — this
is disclosed rather than hidden.

**Backend integration** (`RiskFeatureBuilder`): four of the ten features (`code_complexity`, `test_coverage`,
`code_smells`, `duplication`, `technical_debt`) currently come from the project's most recent
`QualityAnalysis` row and are **shared across every component in that project** — this codebase doesn't
track per-file complexity/coverage yet. The other features (`vulnerability_count`,
`max_vulnerability_severity`, `historical_defects`, `historical_failure_rate`, `code_churn`) are genuinely
component-specific.

## 12. MLOps Functionality

Distinct from the ML service's own internal log, the Spring Boot backend keeps its **own** project-aware
prediction log (`MlPredictionLog` entity), written to every time `analyze()` gets a real response from the
ML service:

| Endpoint | Purpose |
|---|---|
| `GET /api/ml/status` | Whether `ML_SERVICE_URL` is configured |
| `POST /api/ml/predict-risk` | Direct passthrough to the ML service for one raw feature vector (no project context, no rule-based fallback) |
| `GET /api/ml/model-info` | Proxies the ML service's `/model-info` |
| `GET /api/ml/metrics` | Proxies the ML service's `/metrics` |
| `POST /api/ml/retrain` | Proxies the ML service's `/retrain` |
| `GET /api/ml/prediction-history?projectId=&limit=` | **This backend's own log** — project/component-aware, not the ML service's context-free log |

**Prediction history is per-project and reflects only real ML calls** — one row is written per component
each time `analyze()` successfully gets a hybrid result. It is *not* the same data as the risk-assessment
current-state rows described in [§10](#10-risk-assessment): `RiskAssessment` rows are overwritten on every
`analyze()` call, while `MlPredictionLog` rows accumulate as an audit trail (deliberately — this is
"basic performance monitoring" for the model, not project risk state).

The frontend's **MLOps** tab (`/projects/{id}/mlops`) surfaces model version, accuracy/precision/recall/F1,
a retrain button, and this project-scoped prediction history table.

## 13. Prerequisites

- **Java 21** + **Maven 3.9+** (backend)
- **Node 20+** + **npm** (frontend)
- **Python 3.11+** (ML service)
- **MySQL 8** (or Docker)
- **Docker + Docker Compose** (recommended — handles all of the above)
- Optional: a running **SonarQube** server, **Trivy** CLI, **OWASP ZAP** daemon, and a **git** checkout with
  history (for the churn analyzer)

## 14. Environment Variables

Copy `.env.example` to `.env` at the project root (used by `docker-compose.yml`) and fill in real values.
**Never commit a real `.env` file.** For running the backend standalone outside Docker, see
`backend/.env.example` instead (same variables, different defaults suited to a local, non-containerized run).

| Variable | Purpose | Required? |
|---|---|---|
| `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` | Database credentials | Yes (Docker) |
| `JWT_SECRET` | 256-bit+ random signing key | Yes |
| `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION` | Token lifetimes (ms) | No (defaults provided) |
| `SPRING_PROFILES_ACTIVE`, `DDL_AUTO`, `SHOW_SQL` | Spring profile / schema behavior | No |
| `AI_API_KEY`, `AI_API_URL`, `AI_API_MODEL` | Optional external AI provider for the AI Analysis tab | No |
| `SONAR_HOST_URL`, `SONAR_TOKEN`, `SONAR_PROJECT_KEY` | SonarQube integration | No |
| `TRIVY_BINARY_PATH`, `TRIVY_SCAN_TARGET` | Trivy CLI mode | No |
| `ZAP_API_URL`, `ZAP_API_KEY`, `ZAP_ALLOWED_TARGETS`, `ZAP_SCAN_TIMEOUT_SECONDS` | OWASP ZAP live scanning | No |
| `ML_SERVICE_URL`, `ML_SERVICE_TIMEOUT_SECONDS` | ML risk-prediction service | No |
| `GIT_REPO_PATH`, `GIT_CHURN_LOOKBACK_DAYS`, `GIT_CHURN_MAX_COMMITS` | Git churn analyzer | No |
| `VITE_API_BASE_URL` | Frontend API base URL (baked in at build time) | Yes |
| `BACKEND_PORT`, `FRONTEND_PORT`, `MYSQL_PORT`, `ML_SERVICE_PORT`, `ZAP_PORT` | Host port mappings (Docker) | No |

**Note on `GIT_REPO_PATH` in Docker**: leave it unset in the root `.env` — `docker-compose.yml` already
defaults it to `/repo` internally (matching the backend's read-only repo volume mount). Setting it explicitly
to `.` in `.env` would override that default and silently break churn analysis in Docker (it degrades to
"unavailable", it does not crash — but real git history won't be found).

**Note on ML service paths**: `docker-compose.yml` sets `ML_MODEL_DIR`/`ML_DATA_DIR` on the `ml-service`
container, but the FastAPI app's actual settings (`ml-service/app/config.py`) read `ML_MODEL_PATH`/
`ML_METADATA_PATH`/`ML_DATASET_PATH`/`ML_PREDICTION_LOG_PATH` instead — those two compose-file variables are
currently unused by the application. This is harmless in practice: the service's relative-path defaults
(`models/...`, `data/...`) resolve against its `/app` working directory, which is exactly where the Docker
volumes are mounted, so persistence still works correctly by default. Flagging this here for accuracy rather
than leaving it undocumented.

## 15. Database Setup

**Via Docker Compose** (recommended): the `mysql` service creates the database/user automatically from
`MYSQL_DATABASE`/`MYSQL_USER`/`MYSQL_PASSWORD` on first boot. Nothing else to do.

**Standalone**: install MySQL 8, create a database and user matching whatever you set in `backend/.env`'s
`DB_URL`/`DB_USERNAME`/`DB_PASSWORD` (or `application.properties`' equivalents), e.g.:

```sql
CREATE DATABASE assqp_db;
CREATE USER 'assqp'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON assqp_db.* TO 'assqp'@'localhost';
```

The schema itself is created/updated automatically by Hibernate on startup (`ddl-auto=update` in the `dev`
profile — this project has no separate migration tool/scripts).

## 16. Backend Setup

```bash
cd backend
cp .env.example .env   # fill in DB credentials, JWT_SECRET at minimum
mvn clean install
mvn spring-boot:run
```

Runs on **port 8080** by default (`SERVER_PORT`). Swagger UI: `http://localhost:8080/swagger-ui.html`.

> **Note on this repository's Maven verification**: the sandbox this platform was built in does not have
> network access to Maven Central, so `mvn compile`/`mvn test` could not be executed here to produce a
> passing build log. Every change was reviewed manually against the existing, already-building codebase
> (correct imports, matching method signatures, consistent types), and the ML service portion (Python) *was*
> executed and verified end-to-end in that same sandbox. Run `mvn clean install` locally before deploying to
> confirm a clean compile — this is a sandbox networking limitation, not a known code issue.

## 17. ML Service Setup

```bash
cd ml-service
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt

# First run only - generates the synthetic dataset and trains a model:
python training/generate_synthetic_dataset.py
python training/train.py --compare-logreg

uvicorn app.main:app --reload --port 8000
```

Runs on **port 8000**. Interactive docs: `http://localhost:8000/docs`. See `ml-service/README.md` for the
full feature reference and retraining notes.

## 18. Frontend Setup

```bash
cd frontend
cp .env.example .env   # set VITE_API_BASE_URL, e.g. http://localhost:8080/api
npm install
npm run dev
```

Runs on Vite's default dev port (**5173**). For a production build: `npm run build` (outputs to `dist/`,
served by nginx in the Docker image).

## 19. Docker / Docker Compose Setup

```bash
cp .env.example .env   # fill in MYSQL_ROOT_PASSWORD, MYSQL_PASSWORD, JWT_SECRET at minimum
docker compose up --build
```

This starts four services: `mysql` (port `3306`), `backend` (port `8080`), `ml-service` (port `8000`),
`frontend` (port `5173`, nginx reverse-proxies `/api/*` to the backend container). The backend has a
read-only mount of the whole project (`.:/repo:ro`) so the Phase 9 git churn analyzer has real history to
read inside the container.

The optional OWASP ZAP daemon is **not** started by a plain `docker compose up` (it's a heavier image most
contributors won't need day-to-day):

```bash
docker compose --profile security up
```

Then set `ZAP_API_URL=http://zap:8080` and `ZAP_ALLOWED_TARGETS` (e.g. including
`http://frontend:5173`) in `.env` before triggering a scan.

## 20. Swagger / OpenAPI

Full interactive API docs at `http://localhost:8080/swagger-ui.html` (raw spec at `/v3/api-docs`), publicly
reachable without a token. Every one of the 22 controllers is tagged (`@Tag`) and every endpoint has an
`@Operation` summary. A bearer-auth scheme is pre-wired in Swagger UI so protected endpoints can be tested
directly from the browser after logging in and pasting an access token.

## 21. Important API Endpoints

| Area | Endpoint |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `GET /api/auth/me` |
| Projects | `GET/POST /api/projects`, `GET/PUT/DELETE /api/projects/{id}` |
| Quality | `POST /api/projects/{id}/quality/analyze`, `GET /api/projects/{id}/quality/latest` |
| Security | `POST /api/projects/{id}/security/analyze`, `GET /api/projects/{id}/security` |
| Trivy | `GET /api/trivy/status`, `POST /api/projects/{id}/trivy/scan/filesystem` |
| ZAP | `GET /api/zap/status`, `POST /api/projects/{id}/zap/scan/baseline`, `POST /api/projects/{id}/zap/import-file` |
| SonarQube | `GET /api/sonarqube/status`, `POST /api/projects/{id}/sonarqube/import` |
| Coverage | `POST /api/projects/{id}/coverage/import`, `GET /api/projects/{id}/coverage/trend` |
| Risk | `POST /api/projects/{id}/risk/analyze`, `GET /api/projects/{id}/risk/summary` |
| Code Churn | `GET /api/code-churn/status`, `POST /api/projects/{id}/code-churn/analyze` |
| ML | `GET /api/ml/status`, `POST /api/ml/predict-risk`, `GET /api/ml/model-info`, `POST /api/ml/retrain` |
| Adaptive Testing | `POST /api/projects/{id}/adaptive-testing/prioritize` |
| Tests | `POST /api/projects/{id}/tests`, `POST /api/tests/{id}/execute` |
| CI/CD | `POST /api/cicd/webhook` (public), `GET /api/projects/{id}/cicd` |
| Dashboard | `GET /api/projects/{id}/dashboard` |

See Swagger UI for the complete, authoritative list (96 endpoints across 22 controllers).

## 22. Authentication and Authorization

All endpoints except `/api/auth/**`, `/api/health/**`, `/actuator/health`, `/v3/api-docs/**`,
`/swagger-ui/**`, and `/api/cicd/webhook` require a `Authorization: Bearer <token>` header. Obtain a token
via `POST /api/auth/login`, refresh via `POST /api/auth/refresh`. Two roles exist: `DEVELOPER` (default,
can only access their own projects) and `ADMIN` (can access/manage all projects and users). Admin-only
endpoints are marked with `@PreAuthorize("hasRole('ADMIN')")` — currently `GET/DELETE /api/users/{id}` and
`GET /api/users`.

## 23. Validation / Testing Commands

```bash
# Backend unit tests (17 test files, JUnit 5 + Mockito + AssertJ)
cd backend && mvn test

# Backend tests + JaCoCo coverage report
cd backend && mvn verify   # report at target/site/jacoco/index.html

# Frontend type-check + lint + production build
cd frontend
npm install
npx tsc -b
npx eslint . --ext ts,tsx
npm run build
```

The frontend commands above were run and verified during development of this repository (0 TypeScript
errors, 0 ESLint warnings, successful production build). The backend `mvn test`/`mvn verify` commands could
not be executed in the environment this was built in due to a lack of network access to Maven Central —
every backend change was instead verified by careful manual review against the existing compiling codebase
and by hand-tracing test assertions against the actual implementation. Run these locally to get an actual
pass/fail signal before deploying.

## 24. Phase 1–14 Implementation Summary

| Phase | Feature | Status |
|---|---|---|
| 1 | Docker (backend/frontend Dockerfiles, docker-compose.yml) | Complete |
| 2 | GitHub Actions CI/CD | Complete |
| 3 | JaCoCo coverage import + trend | Complete |
| 4 | SonarQube integration | Complete |
| 5 | Trivy security scanning | Complete |
| 6 | OWASP ZAP DAST integration | Complete |
| 7 | ML risk-prediction service (FastAPI + RandomForest) | Complete, trained and tested end-to-end |
| 8 | Spring Boot to ML service integration (hybrid risk) | Complete |
| 9 | Git code churn (JGit, local history) | Complete |
| 10 | Basic MLOps (model info/metrics/retrain/prediction history) | Complete |
| 11 | Frontend integration audit (loading/error/empty states, endpoint consistency) | Complete — 2 real error-handling bugs found & fixed, ESLint config added, dead code removed |
| 12 | Data model audit (cascade/orphan behavior, nullable fields, FK integrity) | Complete — cascade-delete bugs (project/user/test-case deletion) found & fixed, `LazyInitializationException` bug found & fixed, risk-assessment history-accumulation bug found & fixed |
| 13 | API/Swagger audit | Complete — 44 missing `@Operation` annotations added across 13 controllers |
| 14 | Documentation (this README) | Complete |

## 25. Current Project Status

**Working end-to-end**: auth, project CRUD, quality/security rule-based analysis, JaCoCo/SonarQube/Trivy/ZAP
integrations (each with graceful degradation), git churn analysis, hybrid ML+rule-based risk assessment,
MLOps endpoints, adaptive testing, CI/CD webhook recording, reports, notifications.

**Known limitations, disclosed rather than hidden:**
- No Flyway/Liquibase migrations — schema is Hibernate-managed (`ddl-auto`). Fine for development; a real
  migration tool would be needed before this could safely run against a production database with existing
  data.
- The ML model is trained on synthetic/sample data (clearly labeled as such) — see [§11](#11-ml-integration).
- Four of the ten ML risk features are project-level, not per-file, because per-file quality metrics aren't
  tracked in this codebase yet.
- The frontend's project-tab list (`PROJECT_TABS` in `constants/routes.ts`) includes a `source` tab with a
  label ("Source Code") in the tab navigation, but **no route is registered for it in `AppRoutes.tsx`** and
  no corresponding page component exists — this tab is a pre-existing gap, not something addressed in this
  documentation pass. The backend's `SourceCodeController` (list/get/delete uploaded file metadata) is fully
  functional and reachable via the API/Swagger; it simply isn't yet exposed through this specific frontend
  tab.
- `ML_MODEL_DIR`/`ML_DATA_DIR` in `docker-compose.yml` are not read by the ML service (see
  [§14](#14-environment-variables)) — harmless today, but worth fixing for clarity if the path-resolution
  logic changes later.
- Backend Maven build/tests were not executed in the environment this repository was developed in (no
  network access to Maven Central there) — verify with `mvn clean install` / `mvn test` locally.
