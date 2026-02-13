package se.valenzuela.monitoring.notification.event;

import se.valenzuela.monitoring.model.MonitoredService;

import java.time.Instant;

public record ServiceRemovedEvent(
        MonitoredService service,
        Instant timestamp
) implements MonitoringEvent {
}
