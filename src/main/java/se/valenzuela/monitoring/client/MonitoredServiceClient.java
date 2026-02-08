package se.valenzuela.monitoring.client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.service.annotation.GetExchange;

public interface MonitoredServiceClient {

    @GetExchange(url = "/actuator/info")
    ResponseEntity<String> getInfo();

    @GetExchange(url = "/actuator/health")
    ResponseEntity<HealthEndpointResponse> getHealth();


}

