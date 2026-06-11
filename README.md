# myapp-api — Spring Boot Template

A production-ready Spring Boot 3 template you can clone to bootstrap new REST APIs. Includes JWT authentication, PostgreSQL + Flyway, Redis caching, observability, and Docker support.

## Prerequisites

- Java 21
- Docker & Docker Compose (for containerized setup)
- Gradle 8.x (wrapper included)

## Quick Start (Docker)

```bash
cp .env.example .env
# Edit .env — set SECURITY_JWT_SECRET to a strong value

docker compose up --build
```

The API will be available at `http://localhost:8080`.

| Endpoint | URL |
|----------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |
| Prometheus | http://localhost:8080/actuator/prometheus |

## Local Development (without Docker)

1. Start PostgreSQL and Redis locally (or use Docker only for infra):

```bash
docker compose up postgres redis -d
```

2. Copy environment file and run:

```bash
cp .env.example .env
./gradlew bootRun
```

## Renaming for Your Project

After cloning, replace the placeholder identifiers:

| Find | Replace with |
|------|-------------|
| `com.myapp` | your base package (e.g. `com.acme`) |
| `myapp-api` | your artifact name (e.g. `acme-api`) |
| `MyApp` | your app prefix (e.g. `Acme`) |

Files to update:

- `settings.gradle.kts` — root project name
- `build.gradle.kts` — group name
- `src/main/java/com/myapp/` — rename directory structure
- `src/test/java/com/myapp/` — rename directory structure
- `application.yml` — `spring.application.name`
- `docker-compose.yml` — container names (optional)
- `OpenApiConfig.java` — API title/description

IDE tip: use **Replace in Files** across the project for `com.myapp` and `myapp`.

## Profiles

| Profile | Purpose |
|---------|---------|
| `dev` | Local development, human-readable logs, Caffeine cache |
| `test` | H2 in-memory DB for automated tests |
| `prod` | JSON logging, Redis cache, Prometheus metrics |

Activate via `SPRING_PROFILES_ACTIVE` or `-Dspring.profiles.active=prod`.

## Authentication

Register and login to obtain JWT tokens:

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# Use token
curl http://localhost:8080/api/v1/examples \
  -H "Authorization: Bearer <accessToken>"
```

Public endpoints: `/api/v1/auth/**`, `/actuator/health`, `/swagger-ui/**`.

Admin-only endpoint: `GET /api/v1/admin/stats` (requires `ROLE_ADMIN`).

## Environment Variables

See [`.env.example`](.env.example) for all supported variables.

Required for production:

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `REDIS_URL`
- `SECURITY_JWT_SECRET` (minimum 32 characters recommended)

## Running Tests

```bash
./gradlew test
```

Tests use the `test` profile with H2 in-memory database.

## Project Structure

```
src/main/java/com/myapp/
├── config/          # App, OpenAPI, request logging
├── controller/      # REST controllers
├── service/         # Business logic
├── repository/      # Spring Data JPA
├── model/           # JPA entities & enums
├── dto/             # Request/response DTOs
├── exception/       # Custom exceptions & global handler
├── security/        # JWT, auth controller, security config
├── cache/           # Caffeine & Redis cache config
└── util/            # Shared utilities
```

## Database Migrations

Flyway SQL migrations live in `src/main/resources/db/migration/`. Schema changes must be added as new versioned files (e.g. `V4__add_table.sql`).

## OAuth2 (Optional)

Google and GitHub OAuth2 client stubs are configured in `application.yml`. Set `OAUTH2_*` env vars and remove the OAuth2 auto-config exclusion in `application-dev.yml` to enable.

## Future Enhancements

The following are intentionally deferred — add as needed:

- WebSocket support
- WebFlux / WebClient for reactive HTTP calls
- AWS S3 and Cloudinary integrations
- Bucket4j rate limiting
- Kryo serialization, CommonMark processing
- API versioning strategy
- Custom logging annotations

## License

Use freely for your projects.
