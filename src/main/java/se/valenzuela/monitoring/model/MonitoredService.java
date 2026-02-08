package se.valenzuela.monitoring.model;

import lombok.Getter;
import lombok.Setter;
import se.valenzuela.monitoring.client.HealthEndpointResponse;
import se.valenzuela.monitoring.client.InfoEndpointResponse;

import java.time.Instant;

@Getter
@Setter
public class MonitoredService {

    public static final String DEFAULT_INFO_ENDPOINT = "/actuator/info";
    public static final String DEFAULT_HEALTH_ENDPOINT = "/actuator/health";

    private String url;
    private String name;
    private String version;
    private boolean infoStatus;
    private boolean healthStatus;
    private HealthEndpointResponse healthResponse;
    private Instant lastUpdated;
    private String infoEndpoint;
    private String healthEndpoint;

    public MonitoredService(String url) {
        this.url = url;
        this.lastUpdated = Instant.now();
        this.infoEndpoint = DEFAULT_INFO_ENDPOINT;
        this.healthEndpoint = DEFAULT_HEALTH_ENDPOINT;
    }

    public void updateInfo(InfoEndpointResponse info) {
        this.name = info.name();
        this.version = info.version();
        this.infoStatus = true;
        this.lastUpdated = Instant.now();
    }

    public void updateHealth(HealthEndpointResponse health) {
        this.healthResponse = health;
        this.healthStatus = health != null && health.status() != null
                && health.status().equalsIgnoreCase("UP");
        this.lastUpdated = Instant.now();
    }

    public void markDown() {
        this.infoStatus = false;
        this.healthStatus = false;
        this.healthResponse = new HealthEndpointResponse("DOWN");
        this.lastUpdated = Instant.now();
    }
}
