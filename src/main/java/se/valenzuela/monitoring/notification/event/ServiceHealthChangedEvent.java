package se.valenzuela.monitoring.notification.event;

import se.valenzuela.monitoring.model.MonitoredService;

import java.time.Instant;

public record ServiceHealthChangedEvent(
        MonitoredService service,
        boolean previouslyHealthy,
        boolean currentlyHealthy,
        Instant timestamp
) implements MonitoringEvent {

    public boolean wentDown() {
        return previouslyHealthy && !currentlyHealthy;
    }

    public boolean cameUp() {
        return !previouslyHealthy && currentlyHealthy;
    }
}
