package se.valenzuela.monitoring.core.client;

import java.util.List;
import java.util.Map;

public record LoggersResponse(
        List<String> levels,
        Map<String, LoggerLevel> loggers
) {
    public record LoggerLevel(String configuredLevel, String effectiveLevel) {}
}
