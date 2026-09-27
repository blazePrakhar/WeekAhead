WeekAhead
WeekAhead is an AI-assisted weekly time budget optimizer that helps
users plan, track, and rebalance their available time across important
life areas.
The application combines a deterministic weekly allocation engine with
time tracking, neglect detection, rebalancing suggestions, dashboard
analytics, and an optional AI-generated weekly insight.
> **Core principle:** the backend remains the source of truth for weekly
> allocation and analytics. AI is used only as an assisted insight
> layer.
---
Features
Authentication
User registration and login
BCrypt password hashing
JWT-based authentication
Authenticated user context
Protected API routes
Life Areas
Create life areas
Configure importance, minimum, and maximum time constraints
Edit active life areas
Archive life areas using soft-delete behavior
Archived areas are excluded from new allocation recommendations
Weekly Availability
Configure weekly available time
Configure fixed commitments
Calculate discretionary time available for planning
Time Allocation
Generate weekly recommendations
Allocate discretionary time across active life areas
Respect importance weights and minimum/maximum constraints
Store weekly recommendation snapshots for historical correctness
Regenerate recommendations when planning inputs change
Time Tracking
Log actual time spent on life areas
Track duration in minutes
Record date, source, and notes
View weekly time-log history
Update and delete time logs
Insights
Compare recommended time with actual tracked time
Detect neglected areas using the backend's deterministic logic
View historical trends when sufficient weekly data exists
Generate an optional AI-assisted weekly insight
Rebalancing
Identify possible time transfers between life areas
Compare planned allocation with actual tracked time
Present suggestions without automatically changing the user's plan
Dashboard
Weekly availability overview
Recommended time
Actual tracked time
Remaining time
Utilization
Life-area breakdown
UI
React-based responsive interface
Custom WeekAhead design system
Sidebar navigation
Page transitions and motion
Recharts-based data visualization where applicable
---
How the Allocation Works
WeekAhead uses deterministic backend logic for the authoritative weekly
recommendation.
At a high level:
``` text
Available Weekly Time
        ↓
Fixed Commitments
        ↓
Discretionary Time
        ↓
Active Life Areas
        ↓
Importance + Minimum/Maximum Constraints
        ↓
Weekly Recommendation
```
The basic weighted allocation principle is:
``` text
recommended_hours =
    (life_area_importance / total_importance) × discretionary_hours
```
The allocation engine then applies the configured constraints so that
recommendations remain within each life area's minimum and maximum
limits.
The authoritative values are stored as integer minutes.
Weekly recommendation snapshots are retained so that historical weeks
remain consistent even when life areas or planning inputs change later.
---
Neglect Detection
WeekAhead compares actual tracked time against recommended time.
The backend contains deterministic neglect-detection logic that
identifies areas that are being under-served over the relevant analysis
period.
The resulting information is exposed to the Insights UI and can also
contribute to rebalancing suggestions.
---
AI Weekly Insight
AI is deliberately kept separate from the core allocation engine.
The AI layer:
Receives weekly information prepared by the backend
Produces a natural-language weekly insight
Does not directly access MySQL
Does not calculate authoritative weekly allocations
Does not modify the user's planning data
Does not replace deterministic business logic
The backend therefore remains the source of truth.
The current configuration supports Gemini and OpenAI provider
dependencies, while the default configuration uses Gemini.
To enable AI insight generation, configure the required provider
credentials in the environment.
---
Architecture
WeekAhead is implemented as a modular Spring Boot monolith with a React
frontend.
``` text
┌───────────────────────────────┐
│           React UI            │
│      Vite + React Router      │
│   Axios + React Hook Form     │
│        Recharts + Motion      │
└───────────────┬───────────────┘
                │ HTTP / JSON
                ▼
┌────────────────────────────────────────┐
│            Spring Boot API             │
│                                        │
│  Auth       Life Areas   Weekly Plans  │
│  Allocation Time Tracking Insights     │
│  Neglect    Rebalancing Dashboard      │
│  AI Insight                            │
└───────────────┬────────────────────────┘
                │
        ┌───────┴────────┐
        ▼                ▼
┌──────────────┐  ┌──────────────┐
│    MySQL     │  │    Redis     │
│  persistent  │  │ cache / data │
│    data      │  │ infrastructure│
└──────────────┘  └──────────────┘

          Flyway
             │
             ▼
     Database migrations
```
Backend organization
The backend follows feature-oriented packages such as:
``` text
com.weekahead
├── ai
├── allocation
├── analytics
├── auth
├── dashboard
├── health
├── lifearea
├── neglect
├── rebalancing
├── timetracking
└── week
```
This keeps domain logic separated while retaining the simplicity of a
single deployable Spring Boot application.
---
Technology Stack
Frontend
React 19
Vite
React Router
Axios
React Hook Form
Recharts
Lucide React
Motion
Backend
Java 21
Spring Boot 3.5.5
Spring Web
Spring Data JPA / Hibernate
Spring Security
Spring Validation
JWT
Maven
Spring Cache
Database and infrastructure
MySQL 8
Redis 7
Flyway
Docker
Docker Compose
API documentation
SpringDoc OpenAPI
Swagger UI
AI
Google Gemini
OpenAI provider dependency
Testing
JUnit 5
Mockito
Spring Boot Test
MockMvc
Spring Security Test
---
Project Structure
``` text
WeekAhead/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/weekahead/
│   │   │   └── resources/
│   │   ├── test/
│   │   └── pom.xml
│   └── Dockerfile
│
├── database/
│   └── migrations/
│
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── assets/
│   │   ├── components/
│   │   ├── context/
│   │   ├── layouts/
│   │   ├── pages/
│   │   ├── routes/
│   │   └── styles/
│   ├── package.json
│   └── Dockerfile
│
├── .env.example
├── .gitignore
├── .dockerignore
├── docker-compose.yml
└── README.md
```
---
API Overview
The backend exposes REST APIs under `/api`.
Authentication
``` text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```
Life Areas
``` text
POST   /api/life-areas
GET    /api/life-areas
PUT    /api/life-areas/{id}
DELETE /api/life-areas/{id}
```
The `DELETE` operation performs the application's archive/soft-delete
behavior rather than permanent deletion.
Weeks
``` text
POST /api/weeks
GET  /api/weeks/current
GET  /api/weeks/{id}
```
Allocation
``` text
POST /api/weeks/{id}/recommendation/generate
GET  /api/weeks/{id}/allocations
```
Time Tracking
``` text
POST   /api/time-logs
GET    /api/time-logs
PUT    /api/time-logs/{id}
DELETE /api/time-logs/{id}
```
Analytics
``` text
GET /api/analytics
```
Neglect Detection
``` text
GET /api/neglect
```
Rebalancing
``` text
GET /api/rebalancing
```
Dashboard
``` text
GET /api/dashboard/weekly
```
AI Insight
``` text
POST /api/weeks/{weekId}/ai-insight
```
---
Environment Variables
Copy the template:
``` text
.env.example
```
to:
``` text
.env
```
and replace the placeholder values with your local configuration.
Important variables include:
``` text
SERVER_PORT=8080

DB_URL=jdbc:mysql://localhost:3308/weekahead_dev?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=weekahead_app
DB_PASSWORD=CHANGE_ME

MYSQL_ROOT_PASSWORD=CHANGE_ME_ROOT_PASSWORD

FLYWAY_MIGRATIONS_PATH=../database/migrations

JWT_SECRET=CHANGE_ME_JWT_SECRET

AI_PROVIDER=gemini
GEMINI_API_KEY=CHANGE_ME_GEMINI_API_KEY
GEMINI_MODEL=gemini-3.1-flash-lite
```
Never commit `.env` or real credentials to Git.
---
Running with Docker Compose
Prerequisites
Install:
Docker Desktop
Git
1. Configure environment
From the project root:
``` powershell
copy .env.example .env
```
Edit `.env` and configure the required values.
2. Start the application
``` powershell
docker compose up -d
```
3. Check services
``` powershell
docker compose ps
```
Expected services:
``` text
weekahead-mysql
weekahead-redis
weekahead-backend
weekahead-frontend
```
4. Open the application
Frontend:
``` text
http://localhost:5173
```
Backend:
``` text
http://localhost:8080
```
MySQL is exposed locally on:
``` text
localhost:3308
```
Redis is exposed locally on:
``` text
localhost:6379
```
5. Stop the application
``` powershell
docker compose down
```
The MySQL data is stored in the Docker volume:
``` text
weekahead_mysql_data
```
so stopping the containers does not remove the database volume.
---
Running Without Docker
Backend
Prerequisites:
Java 21+
Maven
MySQL
Redis
From the backend directory:
``` powershell
cd backend
mvn spring-boot:run
```
The backend runs on:
``` text
http://localhost:8080
```
Make sure the required environment variables are available in the shell
before starting Spring Boot.
Frontend
Prerequisites:
Node.js
From the frontend directory:
``` powershell
cd frontend
npm install
npm run dev
```
The Vite development server normally runs on:
``` text
http://localhost:5173
```
The API base URL is configured through:
``` text
VITE_API_BASE_URL
```
---
Database Migrations
Database schema changes are managed with Flyway.
Migration files are located under:
``` text
database/migrations/
```
The backend validates the schema using:
``` yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```
This keeps schema creation and changes under version-controlled
migrations rather than relying on Hibernate to modify the production
schema.
---
Swagger / OpenAPI
When the backend is running, Swagger UI is available through SpringDoc's
standard endpoint:
``` text
http://localhost:8080/swagger-ui/index.html
```
OpenAPI JSON is available at:
``` text
http://localhost:8080/v3/api-docs
```
---
Testing
Backend tests
From:
``` text
backend/
```
run:
``` powershell
mvn test
```
The final validation run completed successfully with:
``` text
Tests run: 173
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```
Frontend production build
From:
``` text
frontend/
```
run:
``` powershell
npm run build
```
The production build completes successfully.
The Vite build currently reports a non-blocking warning for a JavaScript
chunk larger than 500 kB. This is a bundle-size optimization warning
rather than a build failure.
---
End-to-End Workflow
The main user workflow is:
``` text
Register / Login
       ↓
Configure Life Areas
       ↓
Configure Weekly Availability
       ↓
Generate Weekly Allocation
       ↓
Track Actual Time
       ↓
Review Insights
       ↓
Review Rebalancing Suggestions
       ↓
Review Weekly Dashboard
       ↓
Generate optional AI Weekly Insight
```
The application does not automatically change the user's plan based on
rebalancing suggestions. Suggestions are presented for review.
---
Security
The application includes:
BCrypt password hashing
JWT authentication
Protected API endpoints
Authenticated user ownership checks
Environment-based secrets
Validation of request data
Centralized exception handling
Database schema validation with Flyway
Secrets should be provided through environment variables rather than
committed to source control.
---
Current Known Limitation
Life Areas use an archive/soft-delete model.
Once a Life Area is archived through the current UI:
It is marked inactive.
It is excluded from new allocation recommendations.
Existing historical allocation data is preserved.
The current UI does not provide an Unarchive/Restore action.
Permanent deletion is also not exposed as a separate user operation.
---
Design and Engineering Principles
WeekAhead intentionally keeps the core planning logic deterministic.
Backend as source of truth
Authoritative calculations and persisted planning data belong to the
backend.
AI as an assistant
AI generates natural-language insight but does not replace the
allocation algorithm.
Integer-minute precision
Time values are represented authoritatively in minutes to avoid
ambiguity caused by floating-point hour calculations.
Historical correctness
Weekly recommendation snapshots preserve the state of recommendations
for completed weeks.
Feature-oriented backend
Domain functionality is organized by feature rather than one large
shared package structure.
Minimal infrastructure
The project uses MySQL, Redis, Docker Compose, and Flyway without
introducing unnecessary microservices or messaging infrastructure.
---
Future Improvements
Potential future enhancements include:
Life Area unarchive/restore
Improved historical trend visualizations after more weekly data
accumulates
Frontend bundle/code-splitting optimization
More extensive end-to-end browser tests
Additional AI provider configuration and fallback behavior
More granular dashboard analytics
Production deployment configuration
