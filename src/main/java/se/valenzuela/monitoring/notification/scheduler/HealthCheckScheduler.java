package se.valenzuela.monitoring.notification.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.notification.event.MonitoringEventCarrier;
import se.valenzuela.monitoring.notification.event.ServiceHealthChangedEvent;
import se.valenzuela.monitoring.core.service.MonitoringService;

import java.time.Duration;
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
    private final ConcurrentHashMap<Long, Instant> lastCheckedTimes = new ConcurrentHashMap<>();

    public HealthCheckScheduler(MonitoringService monitoringService,
                                ApplicationEventPublisher eventPublisher) {
        this.monitoringService = monitoringService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${bootguard.health-check.tick-interval:5000}",
            initialDelayString = "${bootguard.health-check.initial-delay:10000}")
    public void checkHealth() {
        Instant now = Instant.now();
        List<MonitoredService> allServices = monitoringService.getServicesWithEnvironments();

        List<MonitoredService> dueServices = allServices.stream()
                .filter(service -> service.getId() != null && isDue(service, now))
                .toList();

        if (!dueServices.isEmpty()) {
            monitoringService.fetchHealthStatuses(dueServices);
            // Always refresh the UI after polling so cert expiry, last-checked
            // time, and other transient fields update without a page reload.
            monitoringService.notifyListeners(dueServices.get(0));
        }

        Set<Long> currentServiceIds = allServices.stream()
                .map(MonitoredService::getId)
                .collect(Collectors.toSet());

        for (MonitoredService service : dueServices) {
            lastCheckedTimes.put(service.getId(), now);

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
            }
        }

        previousHealthStates.keySet().retainAll(currentServiceIds);
        lastCheckedTimes.keySet().retainAll(currentServiceIds);
    }

    private boolean isDue(MonitoredService service, Instant now) {
        Instant lastChecked = lastCheckedTimes.get(service.getId());
        if (lastChecked == null) {
            return true;
        }
        long intervalSeconds = service.getEffectiveHealthCheckIntervalSeconds();
        return Duration.between(lastChecked, now).getSeconds() >= intervalSeconds;
    }
}
