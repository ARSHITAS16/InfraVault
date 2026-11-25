# InfraVault - Enterprise Infrastructure Inventory & Credential Manager

**InfraVault** is a secure infrastructure inventory and credential management platform featuring split-pane datacenter navigation, AES-256-GCM secret encryption, role-based & datacenter-scoped access control, Excel bulk device import, and UTC operational security audit logs.

---

## Architecture Overview

- **Frontend**: React 19, TypeScript, Vite (`frontend/`), deployed via GitHub Pages (`gh-pages` branch).
- **Backend**: Spring Boot 3.3.5, Java 21, Spring Security (Stateless JWT), Spring Data JPA / Hibernate, Flyway, BouncyCastle, Apache POI (`backend/`), deployed via Docker on Render (`main` branch).
- **Database**: Neon Serverless PostgreSQL (`infravault`, AWS US East 2) with SSL (`sslmode=require`) and conservative HikariCP pooling (`maximum-pool-size=5`, `minimum-idle=0`).

### Role-Based Access Control (RBAC)

| Role | Scope | Capabilities |
| :--- | :--- | :--- |
| `SUPER_ADMIN` | Global | Full access to all datacenters, folders, devices, credentials, audit logs, and user lifecycle management. |
| `DC_ADMIN` | Assigned Datacenters | Create/delete datacenters they administer, assign/revoke datacenter users, copy permissions, manage folders/devices/credentials, and view audit logs for administered datacenters. |
| `OPERATOR` | Assigned Datacenters | Read/write folders, devices, and credentials where granted `WRITE`/`ADMIN` access; read/reveal secrets where granted `READ`+ access. |
| `VIEWER` | Assigned Datacenters | Strictly read-only access to assigned datacenters and authorized secret reveal. Public self-registration defaults to `VIEWER`. |

---

## Required Environment Variables

Copy `.env.example` to `.env` for local runs (`.env` is excluded from Git). Never commit secrets or real connection strings.

| Variable | Description |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` (or `DB_URL`) | Neon JDBC URL, e.g. `jdbc:postgresql://<neon-host>:5432/infravault?sslmode=require&channelBinding=require` |
| `SPRING_DATASOURCE_USERNAME` (or `DB_USERNAME`) | Neon database role username |
| `SPRING_DATASOURCE_PASSWORD` (or `DB_PASSWORD`) | Neon database password |
| `SPRING_PROFILES_ACTIVE` | Set to `prod` in production |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Keep `validate` (Flyway manages schema migrations) |
| `JWT_SECRET` | Random secret key (>= 32 bytes) for HMAC-SHA JWT signing |
| `APP_ENCRYPTION_KEY` | Exactly 32-character AES-256 master key for credential encryption (preserve across deployments to retain decryptability) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed browser origins |
| `ADMIN_INIT_USERNAME` / `ADMIN_INIT_EMAIL` / `ADMIN_INIT_PASSWORD` | Optional one-time initial `SUPER_ADMIN` bootstrap when the `users` table is empty |
| `VITE_API_URL` | Backend base URL for frontend builds |

---

## Database Migrations (Flyway + Neon PostgreSQL)

Schema changes are managed by Flyway in `backend/src/main/resources/db/migration/`:
- `V1__initial_schema.sql`: Creates all core tables (`users`, `datacenters`, `datacenter_users`, `folders`, `devices`, `credentials`, `device_fields`, `audit_logs`, and secondary inventory tables) plus indexes and foreign-key constraints.
- Hibernate is configured with `ddl-auto=validate` so startup verifies the live Neon schema without destructive DDL.

---

## Local Setup & Commands

### Prerequisites
- Java 21 JDK (`JAVA_HOME` configured)
- Node.js 18+ and npm
- Neon PostgreSQL credentials (or local PostgreSQL via `docker compose up -d`)

### 1. Run Backend Locally
```powershell
cd backend
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://<NEON_HOST>:5432/infravault?sslmode=require&channelBinding=require"
$env:SPRING_DATASOURCE_USERNAME="<NEON_USER>"
$env:SPRING_DATASOURCE_PASSWORD="<NEON_PASSWORD>"
.\mvnw.cmd spring-boot:run
```
Backend API runs at `http://localhost:8080` and health check is at `/actuator/health`.

### 2. Run Backend Tests & Build
```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd clean package
```

### 3. Run Frontend Locally, Lint & Build
```powershell
cd frontend
npm ci
npm run dev
npm run lint
npm run build
```

---

## Deployment

### Backend (Render)
1. Connected repository: `ARSHITAS16/InfraVault` (`main` branch), Docker build from `backend/Dockerfile` (`render.yaml`).
2. Configure the environment variables listed above in the Render Dashboard (**never** commit passwords or keys to `render.yaml`).
3. Ensure `APP_ENCRYPTION_KEY` and `JWT_SECRET` match your desired production values before deploying.

### Frontend (GitHub Pages)
```powershell
cd frontend
npm run deploy
```

---

## Security & Troubleshooting

- **Neon idle compute / SSL**: HikariCP uses `maximum-pool-size=5`, `minimum-idle=0`, `max-lifetime=600000`, and `connection-timeout=30000` so connections recover cleanly after Neon auto-suspend.
- **Timezone errors (`Asia/Calcutta`)**: JVM timezone is forced to `UTC` at startup and Hibernate uses `hibernate.jdbc.time_zone=UTC`.
- **No default admin password**: If `users` is empty, set `ADMIN_INIT_USERNAME`, `ADMIN_INIT_EMAIL`, and `ADMIN_INIT_PASSWORD` (>= 8 chars) on first startup, then remove or rotate after login.
