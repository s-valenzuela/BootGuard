# BootGuard

A self-hosted monitoring dashboard for Spring Boot services. BootGuard polls the Actuator `/health` and `/info` endpoints of registered services, tracks health state changes, and dispatches notifications (email, Slack) when something goes wrong.

Built with **Spring Boot 4.0.2**, **Vaadin 25.0.5**, and **Java 25** with virtual threads throughout.

## What it does

- Registers Spring Boot services via a REST endpoint (`POST /register`) or through the UI
- Polls each service's Actuator health endpoint on a configurable interval (default: 30 s, per-service overrides supported)
- Detects `UP` → `DOWN` and `DOWN` → `UP` transitions and fires async notifications
- Sends alerts via **email** (SMTP) and/or **Slack** (webhook) — both channels are optional
- Exposes its own Actuator endpoints (`/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`)
- Serves a Vaadin UI over HTTPS on port 8443

## Tech stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.2 |
| UI | Vaadin 25.0.5 |
| Persistence | Spring Data JPA + Flyway + MariaDB 11 |
| HTTP client | Apache HttpClient 5 via `RestClient` |
| Notifications | Spring Mail + Slack webhook (both optional) |
| Observability | Micrometer + Prometheus |
| Build | Maven Wrapper |
| Runtime | Java 25 with virtual threads |

## Prerequisites

- **Java 25** (`java -version` should report 25)
- **Docker** — runs MariaDB and Mailpit (development only)
- **OpenSSL** — generates the local TLS certificate

## Setup

### 1. Generate a self-signed TLS certificate

The application requires PEM files for HTTPS. Generate them once and place them on the classpath:

```bash
openssl req -x509 -newkey rsa:4096 -sha256 -days 825 \
  -nodes \
  -keyout src/main/resources/key.pem \
  -out src/main/resources/cert.pem \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

| File | Role |
|---|---|
| `src/main/resources/cert.pem` | Certificate (keystore + truststore) |
| `src/main/resources/key.pem` | Private key |

These files are excluded from version control. Browsers will show a security warning for self-signed certificates — accept the exception or import `cert.pem` into your OS trust store.

To inspect the certificate:

```bash
openssl x509 -in src/main/resources/cert.pem -noout -subject -dates -fingerprint
```

### 2. Start infrastructure

```bash
docker compose up -d
```

This starts:

- **MariaDB 11** on port `3306` — database `bootguard`, user `bootguard` / password `bootguard`
- **Mailpit** on port `1025` (SMTP) and `8025` (web UI) — catches outgoing email locally for development. Replace with a real SMTP host in production (see [Notifications](#notifications))

### 3. Build and run

```bash
# Compile
./mvnw compile

# Run tests
./mvnw test

# Start the application
./mvnw spring-boot:run
```

The UI is available at **https://localhost:8443**. Vaadin opens the browser automatically in development mode.

## Registering a Spring Boot service

Any Spring Boot service with Actuator on the classpath can register itself with BootGuard at startup. Add this to your monitored service:

```java
// Runs once after the application context is ready
@Bean
ApplicationRunner registerWithBootGuard(RestClient.Builder builder) {
    return args -> builder.build()
        .post()
        .uri("https://bootguard-host:8443/register")
        .body(Map.of("url", "https://my-service:8080"))
        .retrieve()
        .toBodilessEntity();
}
```

The `POST /register` endpoint accepts:

```json
{ "url": "https://my-service:8080" }
```

BootGuard will immediately discover the service via its `/actuator/info` endpoint and begin polling `/actuator/health`.

Alternatively, services can be added manually from the **Services** view in the UI.

## Notifications

Both notification channels are optional — BootGuard functions as a monitoring dashboard without them.

### Email

Configure an SMTP host via environment variables or `application.yaml`. Mailpit (`localhost:1025`) is used in development to capture outgoing mail without a real mail server; replace it with your actual SMTP relay for production:

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

### Slack

Configure a Slack incoming webhook URL through the **Settings** view in the UI or directly in `application.yaml`:

```yaml
bootguard:
  notifications:
    slack:
      webhook-url: https://hooks.slack.com/services/…
```

Leave the webhook URL unset to disable Slack notifications entirely.

## Configuration reference

Key properties in `application.yaml`:

```yaml
bootguard:
  health-check:
    interval: 30000       # Default polling interval in ms (global)
    initial-delay: 10000  # Delay before first poll after startup

spring:
  mail:
    host: ${SMTP_HOST:localhost}
    port: ${SMTP_PORT:1025}
```

Per-service polling intervals can be overridden in the **Services** view without restarting the application.

## Production build

The production profile pre-builds the Vaadin frontend bundle (required for deployment without Node.js on the server):

```bash
./mvnw -Pproduction package
java -jar target/monitoring-0.0.1-SNAPSHOT.jar
```

## Project structure

```
se.valenzuela.monitoring
├── core/            # Monitoring domain — MonitoredService, health polling
├── notification/    # Events, channels (Email, Slack), scheduler
├── settings/        # Key/value app settings (AppSetting)
├── controller/      # POST /register
├── config/          # RestClient bean, SSL, scheduling, async
└── ui/              # Vaadin views and reusable components
```

Database schema is managed by Flyway. Migrations live in `src/main/resources/db/migration/`.

## Actuator endpoints

BootGuard exposes its own health status at:

| Endpoint | Description |
|---|---|
| `/actuator/health` | Database, mail, and SSL certificate health |
| `/actuator/metrics` | Micrometer metrics |
| `/actuator/prometheus` | Prometheus scrape target |
| `/actuator/info` | Application name, version, description |
