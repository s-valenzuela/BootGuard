package se.valenzuela.monitoring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import se.valenzuela.monitoring.client.HealthEndpointResponse;
import se.valenzuela.monitoring.client.InfoEndpointResponse;
import se.valenzuela.monitoring.model.MonitoredService;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
@Service
public class MonitoringService {

    private final RestClient restClient;
    private final ConcurrentHashMap<String, MonitoredService> services = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<MonitoredService>> listeners = new CopyOnWriteArrayList<>();

    public MonitoringService(RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean addService(String url) {
        MonitoredService service = new MonitoredService(url);
        if (services.putIfAbsent(url, service) != null) {
            return false;
        }
        refreshService(service);
        listeners.forEach(listener -> listener.accept(service));
        return true;
    }

    private void refreshService(MonitoredService service) {
        String baseUrl = service.getUrl();
        try {
            InfoEndpointResponse info = restClient.get()
                    .uri(baseUrl + service.getInfoEndpoint())
                    .retrieve()
                    .body(InfoEndpointResponse.class);
            HealthEndpointResponse health = restClient.get()
                    .uri(baseUrl + service.getHealthEndpoint())
                    .retrieve()
                    .body(HealthEndpointResponse.class);
            service.updateInfo(info);
            service.updateHealth(health);
        } catch (Exception e) {
            log.warn("Endpoint did not respond successfully for {}", baseUrl, e);
            service.markDown();
        }
    }

    public void addListener(Consumer<MonitoredService> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<MonitoredService> listener) {
        listeners.remove(listener);
    }

    @Scheduled(cron = "*/30 * * * * *")
    public void refreshAll() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (MonitoredService service : services.values()) {
                executor.submit(() -> {
                    refreshService(service);
                    listeners.forEach(listener -> listener.accept(service));
                });
            }
        }
    }

    public Collection<MonitoredService> getServices() {
        return Collections.unmodifiableCollection(services.values());
    }

    public void removeService(MonitoredService service) {
        services.remove(service.getUrl());
        listeners.forEach(listener -> listener.accept(service));
    }

}
