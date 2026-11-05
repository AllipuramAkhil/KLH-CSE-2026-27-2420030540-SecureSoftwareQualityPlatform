# ASSQP Frontend

React + TypeScript + Vite frontend for the **AI-Driven Adaptive Software Quality Assurance and Security Automation Platform (ASSQP)**. Connects directly to the existing Spring Boot `assqp-backend` REST API — no mock data, no fabricated endpoints.

## Overview

ASSQP's frontend is a SaaS-style dashboard covering project management, source code upload, quality analysis, security scanning, test-case management, the adaptive testing engine, risk assessment, AI-assisted analysis (with rule-based fallback), reporting, CI/CD pipeline visibility, and notifications — scoped by JWT-authenticated role (`ADMIN`, `DEVELOPER`, `TESTER`, `SECURITY_ANALYST`).

## Architecture

```
React + Vite UI  →  REST + JWT  →  Spring Boot API (assqp-backend)  →  MySQL
```

- **API layer** (`src/api/`) — one module per backend controller, using the exact request/response DTO shapes from the backend.
- **Server state** — TanStack Query (`src/hooks/`) handles caching, invalidation, and refetching. No server data is duplicated in client state.
- **Client UI state** — `src/store/appStore.ts` tracks only the last-opened project ID (localStorage), used to deep-link the sidebar's project-scoped nav items.
- **Auth** — `src/context/AuthContext.tsx` + `src/utils/auth.ts` (token storage) + the axios interceptor in `src/api/axios.ts` (attaches JWT, auto-refreshes on 401).

## Tech Stack

React 18 · TypeScript · Vite 5 · Tailwind CSS · React Router 6 · Axios · TanStack Query 5 · React Hook Form + Zod · Lucide React · Recharts · Framer Motion

## Folder Structure

```
assqp-frontend/
├── src/
│   ├── api/            15 modules — one per backend controller
│   ├── components/
│   │   ├── ui/          Reusable primitives (Button, Input, Card, Badge, Modal, Toast, …)
│   │   ├── layout/       Sidebar, Navbar, ThemeToggle, NotificationBell
│   │   ├── common/       LoadingSpinner, EmptyState, ErrorState, ScoreCard, SearchInput, FilterBar
│   │   └── projects/     SourceCodeUpload (drag-and-drop)
│   ├── pages/           Route-level page components, grouped by domain
│   ├── layouts/          AuthLayout, DashboardLayout, ProjectLayout (project tab nav)
│   ├── hooks/            TanStack Query hooks per domain
│   ├── context/          AuthContext
│   ├── routes/           AppRoutes, ProtectedRoute, RoleRoute
│   ├── types/            TypeScript interfaces mirroring backend DTOs exactly
│   ├── store/            appStore (last-active-project tracking)
│   ├── utils/             auth (token storage), formatters, validators (Zod schemas), risk
│   ├── constants/         routes, roles (nav access map), statuses (badge color tokens)
│   └── styles/            globals.css (Tailwind + CSS variable design tokens)
├── package.json
├── vite.config.ts
├── tsconfig.json / tsconfig.app.json / tsconfig.node.json
├── .env.example
└── .gitignore
```

## Installation

```bash
cd assqp-frontend
npm install
```

## Environment Variables

```bash
cp .env.example .env
```

| Variable | Description | Default |
|---|---|---|
| `VITE_API_BASE_URL` | Base URL of the ASSQP backend REST API | `http://localhost:8080/api` |

## Backend Configuration

This frontend expects the ASSQP Spring Boot backend (`assqp-backend`) to be running and reachable at `VITE_API_BASE_URL`. Start the backend first:

```bash
cd ../assqp-backend
mvn spring-boot:run
```

CORS is already configured on the backend (`CorsConfig.java`) to allow `http://localhost:*` origins.

## Running the Frontend

```bash
npm run dev
```

Opens at `http://localhost:5173` (or the next available port).

## Available Routes

| Route | Page |
|---|---|
| `/login`, `/register`, `/forgot-password` | Auth |
| `/dashboard` | Main KPI dashboard (project-selectable) |
| `/projects`, `/projects/new`, `/projects/:id/edit` | Project CRUD |
| `/projects/:id` | Project overview + source code upload |
| `/projects/:id/quality` | Quality analysis |
| `/projects/:id/security` | Security analysis |
| `/vulnerabilities/:id` | Vulnerability detail panel |
| `/projects/:id/tests`, `/projects/:id/tests/new` | Test case management |
| `/projects/:id/tests/:testId/executions` | Test execution + history |
| `/projects/:id/executions` | Project-wide execution history |
| `/projects/:id/adaptive-testing` | Adaptive testing engine (signature feature) |
| `/projects/:id/risk` | Risk assessment + risk matrix |
| `/projects/:id/ai` | AI analysis workspace (with fallback indicator) |
| `/projects/:id/reports` | Reports |
| `/projects/:id/cicd` | CI/CD pipeline runs |
| `/notifications` | Notifications |
| `/users` | Admin-only user management |
| `/profile` | Current user profile |

## API Integration

Every module in `src/api/` maps 1:1 to a backend controller and uses the exact DTO field names from the backend (verified against the actual `assqp-backend` source — not invented). For example, `qualityApi.ts` calls `GET /projects/{projectId}/quality/latest` and types the response as `QualityAnalysisResponse` with fields `overallQualityScore`, `linesOfCode`, `cyclomaticComplexity`, etc., matching `QualityAnalysisResponse.java` exactly.

Since almost every backend endpoint is project-scoped (`/api/projects/{projectId}/...`), the top-level sidebar items (Quality, Security, Testing, etc.) deep-link into whichever project the user last opened (tracked in `appStore`), rather than pointing to non-existent global routes.

## Authentication

- `POST /api/auth/register`, `POST /api/auth/login` → `AuthResponse { accessToken, refreshToken, tokenType, userId, fullName, email, role }`
- `POST /api/auth/refresh` accepts `{ refreshToken }` and returns a new `AuthResponse` — matches `RefreshRequest.java` / `AuthServiceImpl.refresh()` exactly.
- `GET /api/auth/me` → `UserResponse`, used both on initial load (session restore) and immediately after login/register to populate the auth context.
- The axios response interceptor (`src/api/axios.ts`) automatically retries a failed request once after a successful silent refresh on `401`, and clears the session (dispatching `assqp:unauthorized`) if the refresh itself fails.
- Tokens are stored via `src/utils/auth.ts` (`tokenStorage`) — never passwords, never anything beyond the access/refresh JWT pair the backend issues.

## Role-Based Access

Frontend route guarding (`RoleRoute`) and sidebar filtering (`NAV_ACCESS` in `src/constants/roles.ts`) are **UX conveniences only**. The Spring Boot backend's `@PreAuthorize` annotations and project-ownership checks remain the actual authorization authority — the frontend never assumes a request will succeed just because a nav item is visible.

## Development

```bash
npm run dev      # start dev server with HMR
npm run lint      # run ESLint
```

## Production Build

```bash
npm run build     # tsc -b && vite build → outputs to dist/
npm run preview   # preview the production build locally
```

## Deployment

`npm run build` produces a static `dist/` folder deployable to any static host (Netlify, Vercel, S3 + CloudFront, Nginx, etc.). Set `VITE_API_BASE_URL` to the deployed backend's URL at build time (Vite inlines `import.meta.env.VITE_*` values at build time, so it must be set in the build environment, not just `.env` on the dev machine).

## Troubleshooting

- **"Network error" / can't reach backend** — confirm `assqp-backend` is running and `VITE_API_BASE_URL` in `.env` points to it.
- **401 loops on every request** — the backend's `JWT_SECRET` may have changed since your last login; clear browser storage and log in again.
- **CORS errors in the browser console** — check `assqp-backend`'s `CorsConfig.java` allows your frontend's origin.
- **Admin-only pages show "Access restricted"** — the logged-in user's `role` (from `/api/auth/me`) isn't `ADMIN`; this is enforced by the backend regardless of frontend routing.
