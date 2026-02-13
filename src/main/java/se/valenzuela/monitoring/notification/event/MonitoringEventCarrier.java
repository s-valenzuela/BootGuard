package se.valenzuela.monitoring.notification.event;

import org.springframework.context.ApplicationEvent;

public class MonitoringEventCarrier extends ApplicationEvent {

    private final MonitoringEvent monitoringEvent;

    public MonitoringEventCarrier(Object source, MonitoringEvent monitoringEvent) {
        super(source);
        this.monitoringEvent = monitoringEvent;
    }

    public MonitoringEvent getMonitoringEvent() {
        return monitoringEvent;
    }
}
