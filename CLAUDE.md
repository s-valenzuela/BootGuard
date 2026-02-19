# BootGuard

Spring Boot 4.0.2 + Vaadin 25.0.5 monitoring application for Spring Boot services. Java 25 with virtual threads enabled.

## Build & Run

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

## Project Structure

Domain-driven layout with three bounded contexts:

```
se.valenzuela.monitoring
├── core/                # Core monitoring domain
│   ├── model/           #   MonitoredService, Environment
│   ├── repository/      #   MonitoredServiceRepository, EnvironmentRepository
│   ├── service/         #   MonitoringService, EnvironmentService
│   └── client/          #   InfoEndpointResponse, HealthEndpointResponse
├── notification/        # Notification domain
│   ├── model/           #   NotificationChannelConfig, NotificationServiceOverride
│   ├── channel/         #   NotificationChannel interface + Email, Slack
│   ├── event/           #   Sealed MonitoringEvent hierarchy + MonitoringEventCarrier
│   ├── repository/      #   Notification JPA repositories
│   ├── scheduler/       #   HealthCheckScheduler (@Scheduled)
│   └── service/         #   NotificationConfigService, NotificationDispatcher
├── settings/            # Application settings domain
│   ├── model/           #   AppSetting
│   ├── repository/      #   AppSettingRepository
│   └── service/         #   AppSettingService
├── controller/          # REST API (POST /register)
├── config/              # AppConfig (RestClient bean, @EnableScheduling, @EnableAsync)
└── ui/                  # Vaadin views + ui/component/ for reusable components
```

## Code Conventions

- **Lombok**: `@Getter`, `@Setter`, `@Slf4j` on entities and services
- **Jackson 3.x**: use `tools.jackson.databind` (NOT `com.fasterxml.jackson`)
- **Vaadin views**: extend `Main`, annotate with `@Route` and `@Menu`, use `ViewToolbar` header, add class names `"view-content"`, `LumoUtility.BoxSizing.BORDER`, `"scrollable-page"`
- **Entities**: protected no-arg constructor for JPA, transient fields for computed/live data
- **Repositories**: extend `JpaRepository<Entity, IdType>`
- **Tests**: Mockito with `@ExtendWith(MockitoExtension.class)`, no Spring context loading
- **Database**: MariaDB, Flyway migrations in `src/main/resources/db/migration/` (currently V5)
- **Commit style**: imperative mood, no co-author tag

## Key Technical Details

- **SSL**: PEM-based SSL bundle named `server`, HTTPS on port 8443
- **Outgoing HTTPS**: RestClient configured with Apache HttpClient 5 + SSL bundle truststore
- **Actuator**: health indicators (db, mail, ssl), info via `management.info.env` with Maven resource filtering (`@project.name@`)
- **Notifications**: async event-driven via `ApplicationEventPublisher` and `@Async`
- **Concurrency**: virtual threads for health check polling (`Executors.newVirtualThreadPerTaskExecutor()`)
