package se.valenzuela.monitoring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import se.valenzuela.monitoring.client.HealthEndpointResponse;
import se.valenzuela.monitoring.model.MonitoredService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
public class MonitoringService {

    private final HashMap<String, MonitoredService> services = new HashMap<>();
    private final List<Consumer<MonitoredService>> listeners = new ArrayList<>();

    public boolean addService(String url) {
        // fetch info from spring boot and populate version
        boolean exists = services.containsKey(url);
        if (exists) {
            return false;
        }

        MonitoredService service = new MonitoredService(url, null, (String) null);
        refreshService(service);
        services.put(url, service);
        listeners.forEach(listener -> listener.accept(service));
        return true;
    }

    private void refreshService(MonitoredService service) {
        RestClient restClient = RestClient.builder()
                .baseUrl(service.getUrl())
                .build();

        String rawInfoResponse;
        HealthEndpointResponse healthResponse;
        try {
            rawInfoResponse = restClient.get()
                    .uri(service.getInfoEndpoint())
                    .retrieve()
                    .body(String.class);
            healthResponse = restClient.get()
                    .uri(service.getHealthEndpoint())
                    .retrieve()
                    .body(HealthEndpointResponse.class);
        } catch (Exception e) {
            log.warn("Endpoint did not respond successfully for {}", service.getUrl(), e);
            rawInfoResponse = "{}";
            healthResponse = new HealthEndpointResponse("DOWN");
        }

        service.setHealthResponse(healthResponse);
        service.setInfo(rawInfoResponse);
    }

    public void addListener(Consumer<MonitoredService> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<MonitoredService> listener) {
        listeners.remove(listener);
    }

    @Scheduled(cron = "*/30 * * * * *")
    public void refreshAll() {
        for (MonitoredService service : services.values()) {
            refreshService(service);
            listeners.forEach(listener -> listener.accept(service));
        }
    }

    public Collection<MonitoredService> getServices() {
        return services.values();
    }

    public void removeService(MonitoredService service) {
        services.remove(service.getUrl());
        listeners.forEach(listener -> listener.accept(service));
    }

}
