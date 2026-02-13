package se.valenzuela.monitoring.notification.event;

import se.valenzuela.monitoring.model.MonitoredService;

import java.time.Instant;

public sealed interface MonitoringEvent
        permits ServiceHealthChangedEvent, ServiceAddedEvent, ServiceRemovedEvent {

    MonitoredService service();

    Instant timestamp();
}
