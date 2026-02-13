package se.valenzuela.monitoring.notification.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import se.valenzuela.monitoring.model.MonitoredService;
import se.valenzuela.monitoring.notification.event.MonitoringEventCarrier;
import se.valenzuela.monitoring.notification.event.ServiceHealthChangedEvent;
import se.valenzuela.monitoring.service.MonitoringService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthCheckSchedulerTest {

    @Mock
    private MonitoringService monitoringService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private HealthCheckScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new HealthCheckScheduler(monitoringService, eventPublisher);
    }

    private MonitoredService createService(Long id, String name, boolean healthy) {
        var service = new MonitoredService("http://localhost:808" + id);
        service.setName(name);
        // Use reflection-free approach: set id via setter (Lombok @Setter)
        service.setId(id);
        service.setHealthStatus(healthy);
        return service;
    }

    @Test
    void firstRun_recordsStateButNoEvent() {
        var service = createService(1L, "app", true);
        when(monitoringService.getServices()).thenReturn(List.of(service));

        scheduler.checkHealth();

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void stableState_noEvent() {
        var service = createService(1L, "app", true);
        when(monitoringService.getServices()).thenReturn(List.of(service));

        scheduler.checkHealth(); // first observation
        scheduler.checkHealth(); // same state

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void upToDown_publishesEvent() {
        var serviceUp = createService(1L, "app", true);
        when(monitoringService.getServices()).thenReturn(List.of(serviceUp));
        scheduler.checkHealth(); // first observation

        var serviceDown = createService(1L, "app", false);
        when(monitoringService.getServices()).thenReturn(List.of(serviceDown));
        scheduler.checkHealth(); // state change

        ArgumentCaptor<MonitoringEventCarrier> captor = ArgumentCaptor.forClass(MonitoringEventCarrier.class);
        verify(eventPublisher).publishEvent(captor.capture());

        var event = (ServiceHealthChangedEvent) captor.getValue().getMonitoringEvent();
        assertTrue(event.wentDown());
        assertFalse(event.cameUp());
        assertTrue(event.previouslyHealthy());
        assertFalse(event.currentlyHealthy());
    }

    @Test
    void downToUp_publishesEvent() {
        var serviceDown = createService(1L, "app", false);
        when(monitoringService.getServices()).thenReturn(List.of(serviceDown));
        scheduler.checkHealth(); // first observation

        var serviceUp = createService(1L, "app", true);
        when(monitoringService.getServices()).thenReturn(List.of(serviceUp));
        scheduler.checkHealth(); // state change

        ArgumentCaptor<MonitoringEventCarrier> captor = ArgumentCaptor.forClass(MonitoringEventCarrier.class);
        verify(eventPublisher).publishEvent(captor.capture());

        var event = (ServiceHealthChangedEvent) captor.getValue().getMonitoringEvent();
        assertFalse(event.wentDown());
        assertTrue(event.cameUp());
    }

    @Test
    void staleEntries_cleanedUp() {
        var service1 = createService(1L, "app1", true);
        var service2 = createService(2L, "app2", true);
        when(monitoringService.getServices()).thenReturn(List.of(service1, service2));
        scheduler.checkHealth(); // record both

        // service2 removed
        when(monitoringService.getServices()).thenReturn(List.of(service1));
        scheduler.checkHealth();

        // Now re-add service2 — should be treated as first observation (no event)
        var service2Again = createService(2L, "app2", false);
        when(monitoringService.getServices()).thenReturn(List.of(service1, service2Again));
        scheduler.checkHealth();

        verifyNoInteractions(eventPublisher);
    }
}
