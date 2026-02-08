package se.valenzuela.monitoring.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import se.valenzuela.monitoring.client.HealthEndpointResponse;
import se.valenzuela.monitoring.client.InfoEndpointResponse;

import java.time.Instant;

@Slf4j
@Getter
@Setter
@AllArgsConstructor
public class MonitoredService {

    public static final String DEFAULT_INFO_ENDPOINT = "/actuator/info";
    public static final String DEFAULT_HEALTH_ENDPOINT = "/actuator/health";

    private String url;
    private String name;
    private String version;
    private boolean infoStatus;
    private String infoResponse;
    private boolean healthStatus;
    private HealthEndpointResponse healthResponse;
    private Instant lastUpdated;
    private String infoEndpoint;
    private String healthEndpoint;

    public MonitoredService(String url, String name, String version) {
        this.url = url;
        this.name = name;
        this.version = version;
        this.lastUpdated = Instant.now();
        this.infoEndpoint = DEFAULT_INFO_ENDPOINT;
        this.healthEndpoint = DEFAULT_HEALTH_ENDPOINT;
    }

    public MonitoredService(String url, String name, String version, boolean infoStatus) {
        this.url = url;
        this.name = name;
        this.version = version;
        this.infoStatus = infoStatus;
        this.lastUpdated = Instant.now();
        this.infoEndpoint = DEFAULT_INFO_ENDPOINT;
        this.healthEndpoint = DEFAULT_HEALTH_ENDPOINT;
    }

    public MonitoredService(String url, String rawString, HealthEndpointResponse healthStatus) {
        InfoEndpointResponse info = parseRawInfoResponse(rawString);

        this.url = url;
        this.name = info.name();
        this.version = info.version();
        this.lastUpdated = Instant.now();
        this.infoStatus = true;
        this.healthResponse = healthStatus;
        this.healthStatus = healthResponse.status().equalsIgnoreCase("UP");
        this.infoEndpoint = DEFAULT_INFO_ENDPOINT;
        this.healthEndpoint = DEFAULT_HEALTH_ENDPOINT;
    }

    public void setInfo(String rawString) {
        InfoEndpointResponse info = parseRawInfoResponse(rawString);
        if (info != null) {
            this.name = info.name();
            this.version = info.version();
            this.infoStatus = true;
            this.lastUpdated = Instant.now();
            this.infoResponse = rawString;
            this.healthStatus = healthResponse != null && healthResponse.status() != null
                    && healthResponse.status().equalsIgnoreCase("UP");
        } else {
            this.infoStatus = false;
            this.healthStatus = false;
        }
    }

    private InfoEndpointResponse parseRawInfoResponse(String rawString) {
        ObjectMapper mapper = new ObjectMapper();
        InfoEndpointResponse info = null;
        try {
            info = mapper.readValue(rawString, InfoEndpointResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing raw response", e);
            info = new InfoEndpointResponse();
        }
        return info;
    }
}
