package se.valenzuela.monitoring.notification.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.valenzuela.monitoring.model.MonitoredService;
import se.valenzuela.monitoring.notification.event.MonitoringEventCarrier;
import se.valenzuela.monitoring.notification.event.ServiceHealthChangedEvent;
import se.valenzuela.monitoring.service.MonitoringService;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HealthCheckScheduler {

    private final MonitoringService monitoringService;
    private final ApplicationEventPublisher eventPublisher;
    private final ConcurrentHashMap<Long, Boolean> previousHealthStates = new ConcurrentHashMap<>();

    public HealthCheckScheduler(MonitoringService monitoringService,
                                ApplicationEventPublisher eventPublisher) {
        this.monitoringService = monitoringService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${bootguard.health-check.interval:30000}",
            initialDelayString = "${bootguard.health-check.initial-delay:10000}")
    public void checkHealth() {
        List<MonitoredService> services = monitoringService.getServices();

        Set<Long> currentServiceIds = services.stream()
                .map(MonitoredService::getId)
                .collect(Collectors.toSet());

        for (MonitoredService service : services) {
            if (service.getId() == null) {
                continue;
            }

            boolean currentlyHealthy = service.isHealthStatus();
            Boolean previouslyHealthy = previousHealthStates.put(service.getId(), currentlyHealthy);

            if (previouslyHealthy == null) {
                log.debug("First observation for service '{}' (id={}): healthy={}",
                        service.getName(), service.getId(), currentlyHealthy);
                continue;
            }

            if (previouslyHealthy != currentlyHealthy) {
                log.info("Health state changed for service '{}' (id={}): {} -> {}",
                        service.getName(), service.getId(), previouslyHealthy, currentlyHealthy);

                var event = new ServiceHealthChangedEvent(
                        service, previouslyHealthy, currentlyHealthy, Instant.now());
                eventPublisher.publishEvent(new MonitoringEventCarrier(this, event));
                monitoringService.notifyListeners(service);
            }
        }

        previousHealthStates.keySet().retainAll(currentServiceIds);
    }
}
