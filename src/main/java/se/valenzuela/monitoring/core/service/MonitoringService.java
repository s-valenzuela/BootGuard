package se.valenzuela.monitoring.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import se.valenzuela.monitoring.core.client.HealthEndpointResponse;
import se.valenzuela.monitoring.core.client.InfoEndpointResponse;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.notification.event.MonitoringEventCarrier;
import se.valenzuela.monitoring.notification.event.ServiceAddedEvent;
import se.valenzuela.monitoring.notification.event.ServiceRemovedEvent;
import se.valenzuela.monitoring.core.repository.MonitoredServiceRepository;
import se.valenzuela.monitoring.settings.service.AppSettingService;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
@Service
public class MonitoringService {

    private final RestClient restClient;
    private final MonitoredServiceRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final AppSettingService appSettingService;
    private final CopyOnWriteArrayList<Consumer<MonitoredService>> listeners = new CopyOnWriteArrayList<>();

    public MonitoringService(RestClient restClient, MonitoredServiceRepository repository,
                             ApplicationEventPublisher eventPublisher, AppSettingService appSettingService) {
        this.restClient = restClient;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.appSettingService = appSettingService;
    }

    public boolean addService(String url) {
        return addServiceWithEndpoints(url, null, null) != null;
    }

    public MonitoredService addServiceWithEndpoints(String url, String infoEndpoint, String healthEndpoint) {
        if (repository.existsByUrl(url)) {
            return null;
        }
        MonitoredService service = new MonitoredService(url);
        if (infoEndpoint != null && !infoEndpoint.isBlank()) {
            service.setInfoEndpoint(infoEndpoint);
        }
        if (healthEndpoint != null && !healthEndpoint.isBlank()) {
            service.setHealthEndpoint(healthEndpoint);
        }
        try {
            InfoEndpointResponse info = restClient.get()
                    .uri(url + service.getInfoEndpoint())
                    .retrieve()
                    .body(InfoEndpointResponse.class);
            service.updateInfo(info);
        } catch (Exception e) {
            log.warn("Could not fetch info for {}", url, e);
        }
        repository.save(service);
        listeners.forEach(listener -> listener.accept(service));
        eventPublisher.publishEvent(new MonitoringEventCarrier(this,
                new ServiceAddedEvent(service, Instant.now())));
        return service;
    }

    public void addListener(Consumer<MonitoredService> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<MonitoredService> listener) {
        listeners.remove(listener);
    }

    public List<MonitoredService> getServices() {
        List<MonitoredService> services = repository.findAll();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (MonitoredService service : services) {
                executor.submit(() -> fetchStatus(service));
            }
        }
        return services;
    }

    private void fetchStatus(MonitoredService service) {
        String baseUrl = service.getUrl();
        try {
            InfoEndpointResponse info = restClient.get()
                    .uri(baseUrl + service.getInfoEndpoint())
                    .retrieve()
                    .body(InfoEndpointResponse.class);
            service.setInfoStatus(true);
            if (info != null) {
                service.setName(info.name());
                service.setVersion(info.version());
            }
        } catch (Exception e) {
            service.setInfoStatus(false);
        }
        try {
            HealthEndpointResponse health = restClient.get()
                    .uri(baseUrl + service.getHealthEndpoint())
                    .retrieve()
                    .body(HealthEndpointResponse.class);
            service.setHealthResponseStatus(health != null ? health.status() : null);
            service.setHealthStatus(health != null && "UP".equalsIgnoreCase(health.status()));
            if (health != null) {
                extractCertificateExpiry(service, health);
            }
        } catch (Exception e) {
            service.setHealthStatus(false);
            service.setHealthResponseStatus("DOWN");
        }
        service.setLastUpdated(Instant.now());
    }

    public void notifyListeners(MonitoredService service) {
        listeners.forEach(listener -> listener.accept(service));
    }

    public void updateServiceUrl(MonitoredService service, String newUrl) {
        service.setUrl(newUrl);
        repository.save(service);
        listeners.forEach(listener -> listener.accept(service));
    }

    public void removeService(MonitoredService service) {
        eventPublisher.publishEvent(new MonitoringEventCarrier(this,
                new ServiceRemovedEvent(service, Instant.now())));
        repository.delete(service);
        listeners.forEach(listener -> listener.accept(service));
    }

    private void extractCertificateExpiry(MonitoredService service, HealthEndpointResponse health) {
        Map<String, JsonNode> components = health.components();
        if (components == null) {
            return;
        }
        JsonNode sslNode = components.get("ssl");
        if (sslNode == null || !sslNode.has("details")) {
            return;
        }
        Instant earliest = null;
        JsonNode details = sslNode.get("details");
        for (JsonNode bundle : details) {
            JsonNode certChain = bundle.get("certificateChain");
            if (certChain == null) {
                continue;
            }
            for (JsonNode aliasEntry : certChain) {
                for (JsonNode cert : aliasEntry) {
                    JsonNode validityEnds = cert.get("validityEnds");
                    if (validityEnds != null && validityEnds.isTextual()) {
                        try {
                            Instant expiry = Instant.parse(validityEnds.asString());
                            if (earliest == null || expiry.isBefore(earliest)) {
                                earliest = expiry;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
        if (earliest != null) {
            service.setEarliestCertExpiry(earliest);
            int warningDays = appSettingService.getCertExpiryWarningDays();
            service.setCertExpiringSoon(earliest.isBefore(Instant.now().plus(warningDays, ChronoUnit.DAYS)));
        }
    }
}
