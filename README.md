# BootGuard

Spring Boot 4.0.2 + Vaadin 25.0.5 monitoring application for Spring Boot services.

## Prerequisites

- Java 25
- Docker (for MariaDB and Mailpit)
- OpenSSL

## SSL Certificate Setup

The application requires a self-signed TLS certificate for local development.
The PEM files are **not** committed to the repository — generate them once before running:

```bash
openssl req -x509 -newkey rsa:4096 -sha256 -days 825 \
  -nodes \
  -keyout src/main/resources/key.pem \
  -out src/main/resources/cert.pem \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

This produces:

| File | Purpose |
|------|---------|
| `src/main/resources/cert.pem` | Self-signed certificate (used as keystore and truststore) |
| `src/main/resources/key.pem` | Private key |

To inspect the generated certificate:

```bash
openssl x509 -in src/main/resources/cert.pem -noout -subject -dates -fingerprint
```

> **Browser trust**: browsers will show a security warning for self-signed certificates.
> Accept the exception, or import `cert.pem` into your OS/browser trust store to suppress it.

## Running the Application

```bash
# Start dependencies (MariaDB + Mailpit)
docker compose up -d

# Build
./mvnw compile

# Run tests
./mvnw test

# Run application (https://localhost:8443)
./mvnw spring-boot:run
```
