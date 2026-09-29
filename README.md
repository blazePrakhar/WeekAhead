<div align="center">

# WeekAhead

**An AI-assisted weekly time budget optimizer.**
Plan, track, and rebalance your time across the areas of life that matter most.

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.5-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-646CFF?logo=vite&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Tests](https://img.shields.io/badge/backend_tests-173_passing-brightgreen)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [How It Works](#how-it-works)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Database Migrations](#database-migrations)
- [Testing](#testing)
- [Security](#security)
- [Design Principles](#design-principles)
- [Known Limitations](#known-limitations)
- [Roadmap](#roadmap)

---

## Overview

WeekAhead helps you decide where your time should go each week, and shows how closely reality matches the plan. You define the life areas you care about (health, career, family, learning, and so on), set how much time you have available, and WeekAhead produces a weekly allocation. As you log time, it surfaces neglected areas and suggests rebalancing moves.

> **Core principle:** the backend is the single source of truth for allocation and analytics. AI is used only as an optional, assistive insight layer and never changes your data.

## Key Features

| Area | Capabilities |
| --- | --- |
| **Authentication** | Registration and login, BCrypt password hashing, JWT-secured API, protected routes |
| **Life Areas** | Create and edit areas with importance, minimum, and maximum time constraints; archive via soft delete |
| **Weekly Availability** | Set available weekly time and fixed commitments; discretionary time is calculated automatically |
| **Allocation** | Importance-weighted recommendations that respect min/max limits; regenerate when inputs change; stored as historical snapshots |
| **Time Tracking** | Log minutes with date, source, and notes; view, edit, and delete weekly history |
| **Insights** | Recommended vs. actual comparison, deterministic neglect detection, historical trends |
| **Rebalancing** | Suggested time transfers between areas, presented for review and never applied automatically |
| **Dashboard** | Availability, recommended, actual, remaining, utilization, and per-area breakdown |
| **AI Weekly Insight** | Optional natural-language summary generated from backend-prepared data (Gemini by default) |
| **UI** | Responsive React interface, custom design system, sidebar navigation, page transitions, Recharts visualizations |

## How It Works

```text
Available Weekly Time
        │  minus
        ▼
Fixed Commitments
        │  equals
        ▼
Discretionary Time
        │  distributed across
        ▼
Active Life Areas
        │  weighted by importance, bounded by min/max
        ▼
Weekly Recommendation (stored as a snapshot)
```

The baseline weighting is proportional to importance:

```text
recommended_time = (area_importance / total_importance) × discretionary_time
```

The allocation engine then enforces each area's configured minimum and maximum. All authoritative values are stored as **integer minutes**, and each generated recommendation is persisted as a **snapshot**, so past weeks remain accurate even if life areas or inputs change later.

**Neglect detection** compares actual tracked time with recommended time over the analysis period and flags areas that are consistently under-served. The results feed both the Insights UI and the rebalancing suggestions.

**AI insight** is deliberately isolated. The AI layer receives a summary prepared by the backend and returns text. It has no database access, does not compute allocations, and does not modify planning data.

## Architecture

WeekAhead is a modular Spring Boot monolith with a React single-page frontend.

```text
┌───────────────────────────────┐
│           React UI            │
│      Vite + React Router      │
│   Axios + React Hook Form     │
│      Recharts + Motion        │
└───────────────┬───────────────┘
                │ HTTP / JSON
                ▼
┌──────────────────────────────────────────┐
│             Spring Boot API              │
│                                          │
│  Auth        Life Areas    Weekly Plans  │
│  Allocation  Time Tracking Insights      │
│  Neglect     Rebalancing   Dashboard     │
│  AI Insight                              │
└───────────────┬──────────────────────────┘
                │
        ┌───────┴────────┐
        ▼                ▼
   ┌─────────┐      ┌─────────┐
   │  MySQL  │      │  Redis  │
   └─────────┘      └─────────┘
        ▲
     Flyway migrations
```

**Backend packages** (`com.weekahead`): `ai`, `allocation`, `analytics`, `auth`, `dashboard`, `health`, `lifearea`, `neglect`, `rebalancing`, `timetracking`, `week`.

Feature-oriented packaging keeps domain logic separated while preserving the simplicity of a single deployable service.

<details>
<summary><strong>Repository layout</strong></summary>

```text
WeekAhead/
├── backend/                 Spring Boot application
│   ├── src/main/java/com/weekahead/
│   ├── src/main/resources/
│   ├── src/test/
│   ├── pom.xml
│   └── Dockerfile
├── database/
│   └── migrations/          Flyway SQL migrations
├── docker/
│   └── mysql/               MySQL init scripts
├── frontend/                React + Vite application
│   ├── src/
│   │   ├── api/  assets/  components/  context/
│   │   └── layouts/  pages/  routes/  styles/
│   ├── package.json
│   └── Dockerfile
├── .env.example
├── docker-compose.yml
└── README.md
```

</details>

## Tech Stack

| Layer | Technologies |
| --- | --- |
| **Frontend** | React 19, Vite, React Router, Axios, React Hook Form, Recharts, Lucide React, Motion |
| **Backend** | Java 21, Spring Boot 3.5.5, Spring Web, Spring Data JPA / Hibernate, Spring Security, Spring Validation, Spring Cache, JWT, Maven |
| **Data & Infra** | MySQL 8, Redis 7, Flyway, Docker, Docker Compose |
| **API Docs** | SpringDoc OpenAPI, Swagger UI |
| **AI** | Google Gemini (default), OpenAI provider dependency |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, MockMvc, Spring Security Test |

## Getting Started

### Option A: Docker Compose (recommended)

**Prerequisites:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) and Git.

```bash
# 1. Clone the repository
git clone https://github.com/blazePrakhar/WeekAhead.git
cd WeekAhead

# 2. Create your environment file
cp .env.example .env        # PowerShell: copy .env.example .env
# Edit .env and replace every CHANGE_ME value

# 3. Build and start all services
docker compose up -d

# 4. Verify
docker compose ps
```

Expected containers: `weekahead-mysql`, `weekahead-redis`, `weekahead-backend`, `weekahead-frontend`.

| Service | URL |
| --- | --- |
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| MySQL (host) | `localhost:3308` |
| Redis (host) | `localhost:6379` |

Stop the stack with `docker compose down`. Database data persists in the `weekahead_mysql_data` volume.

### Option B: Run locally without Docker

**Prerequisites:** Java 21+, Maven, Node.js, MySQL 8, Redis 7.

**Backend**

```bash
cd backend
mvn spring-boot:run
```

Ensure the variables from `.env` are exported in your shell first. The API starts on `http://localhost:8080`.

**Frontend**

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

The dev server starts on `http://localhost:5173`. The API URL is set with `VITE_API_BASE_URL`.

## Configuration

Copy `.env.example` to `.env` and set your own values.

| Variable | Description | Example / Default |
| --- | --- | --- |
| `SERVER_PORT` | Backend HTTP port | `8080` |
| `DB_URL` | JDBC URL (local, non-Docker runs) | `jdbc:mysql://localhost:3308/weekahead_dev?...` |
| `DB_USERNAME` | Database user | `weekahead_app` |
| `DB_PASSWORD` | Database password | `CHANGE_ME` |
| `MYSQL_ROOT_PASSWORD` | MySQL root password (Docker) | `CHANGE_ME_ROOT_PASSWORD` |
| `FLYWAY_MIGRATIONS_PATH` | Location of migration files | `../database/migrations` |
| `JWT_SECRET` | Secret used to sign JWTs | `CHANGE_ME_JWT_SECRET` |
| `AI_PROVIDER` | AI provider | `gemini` |
| `GEMINI_API_KEY` | Gemini API key (required for AI insight) | `CHANGE_ME_GEMINI_API_KEY` |
| `GEMINI_MODEL` | Gemini model name | `gemini-3.1-flash-lite` |
| `VITE_API_BASE_URL` | API base URL used by the frontend | `http://localhost:8080` |

> ⚠️ **Never commit `.env` or real credentials.** Only `.env.example` should be tracked. AI insight generation is optional; the rest of the app works without an API key.

## API Reference

All endpoints are served under `/api` and, except registration and login, require a `Bearer` JWT. Full interactive documentation is available in Swagger UI, and the OpenAPI spec is at `/v3/api-docs`.

| Domain | Method | Endpoint | Description |
| --- | --- | --- | --- |
| **Auth** | `POST` | `/api/auth/register` | Register a new user |
| | `POST` | `/api/auth/login` | Log in and receive a JWT |
| | `GET` | `/api/auth/me` | Get the authenticated user |
| **Life Areas** | `POST` | `/api/life-areas` | Create a life area |
| | `GET` | `/api/life-areas` | List life areas |
| | `PUT` | `/api/life-areas/{id}` | Update a life area |
| | `DELETE` | `/api/life-areas/{id}` | Archive (soft delete) a life area |
| **Weeks** | `POST` | `/api/weeks` | Create or configure a week |
| | `GET` | `/api/weeks/current` | Get the current week |
| | `GET` | `/api/weeks/{id}` | Get a week by ID |
| **Allocation** | `POST` | `/api/weeks/{id}/recommendation/generate` | Generate the weekly recommendation |
| | `GET` | `/api/weeks/{id}/allocations` | Get stored allocations |
| **Time Logs** | `POST` | `/api/time-logs` | Create a time log |
| | `GET` | `/api/time-logs` | List time logs |
| | `PUT` | `/api/time-logs/{id}` | Update a time log |
| | `DELETE` | `/api/time-logs/{id}` | Delete a time log |
| **Analytics** | `GET` | `/api/analytics` | Recommended vs. actual and trends |
| **Neglect** | `GET` | `/api/neglect` | Neglected-area detection |
| **Rebalancing** | `GET` | `/api/rebalancing` | Suggested time transfers |
| **Dashboard** | `GET` | `/api/dashboard/weekly` | Weekly dashboard summary |
| **AI** | `POST` | `/api/weeks/{weekId}/ai-insight` | Generate an AI weekly insight |

## Database Migrations

Schema changes are versioned with **Flyway** and live in `database/migrations/` (`V1__foundation.sql` through `V9__create_audit_logs_table.sql`). Hibernate runs in `ddl-auto: validate` mode, so it verifies the schema but never modifies it.

## Testing

**Backend** (from `backend/`):

```bash
mvn test
```

Latest validation run:

```text
Tests run: 173, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Frontend production build** (from `frontend/`):

```bash
npm run build
```

The build succeeds. Vite reports a non-blocking warning for a JavaScript chunk larger than 500 kB, which is a bundle-size optimization opportunity rather than an error.

## Security

- BCrypt password hashing
- Stateless JWT authentication with protected endpoints
- Per-user ownership checks on all user data
- Request validation and centralized exception handling
- Secrets supplied through environment variables
- Schema integrity enforced through Flyway and Hibernate validation

## Design Principles

- **Backend as source of truth.** Authoritative calculations and persisted planning data live in the backend.
- **AI as an assistant.** AI writes insight; it never replaces the deterministic algorithm or edits data.
- **Integer-minute precision.** Minutes avoid floating-point ambiguity in hour-based math.
- **Historical correctness.** Weekly snapshots keep completed weeks consistent.
- **Feature-oriented modules.** Code is organized by domain, not by technical layer.
- **Minimal infrastructure.** MySQL, Redis, Docker Compose, and Flyway, with no unnecessary microservices or message brokers.

## Known Limitations

Life Areas use an archive model. Once archived, an area is marked inactive, excluded from new recommendations, and its historical allocation data is preserved. The current UI does not yet offer an **Unarchive/Restore** action, and permanent deletion is not exposed.

## Roadmap

- [ ] Life Area unarchive / restore
- [ ] Richer historical trend visualizations as data accumulates
- [ ] Frontend code splitting and bundle optimization
- [ ] Broader end-to-end browser test coverage
- [ ] Additional AI providers with fallback behavior
- [ ] More granular dashboard analytics
- [ ] Production deployment configuration

---

<div align="center">

Built with Spring Boot and React.

</div>
