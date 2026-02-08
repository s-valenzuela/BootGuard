package se.valenzuela.monitoring.model;

import org.junit.jupiter.api.Test;
import se.valenzuela.monitoring.client.HealthEndpointResponse;
import se.valenzuela.monitoring.client.InfoEndpointResponse;

import static org.junit.jupiter.api.Assertions.*;

class MonitoredServiceTest {

    @Test
    void constructor_setsDefaults() {
        MonitoredService service = new MonitoredService("http://localhost:8080");

        assertEquals("http://localhost:8080", service.getUrl());
        assertNull(service.getName());
        assertNull(service.getVersion());
        assertFalse(service.isInfoStatus());
        assertFalse(service.isHealthStatus());
        assertNotNull(service.getLastUpdated());
        assertEquals(MonitoredService.DEFAULT_INFO_ENDPOINT, service.getInfoEndpoint());
        assertEquals(MonitoredService.DEFAULT_HEALTH_ENDPOINT, service.getHealthEndpoint());
    }

    @Test
    void updateInfo_setsFieldsAndMarksUp() {
        MonitoredService service = new MonitoredService("http://localhost:8080");
        var info = new InfoEndpointResponse("my-app", "My App", "2.0.0");

        service.updateInfo(info);

        assertEquals("my-app", service.getName());
        assertEquals("2.0.0", service.getVersion());
        assertTrue(service.isInfoStatus());
    }

    @Test
    void updateHealth_statusUp() {
        MonitoredService service = new MonitoredService("http://localhost:8080");

        service.updateHealth(new HealthEndpointResponse("UP"));

        assertTrue(service.isHealthStatus());
        assertEquals("UP", service.getHealthResponse().status());
    }

    @Test
    void updateHealth_statusDown() {
        MonitoredService service = new MonitoredService("http://localhost:8080");

        service.updateHealth(new HealthEndpointResponse("DOWN"));

        assertFalse(service.isHealthStatus());
    }

    @Test
    void updateHealth_nullResponse() {
        MonitoredService service = new MonitoredService("http://localhost:8080");

        service.updateHealth(null);

        assertFalse(service.isHealthStatus());
    }

    @Test
    void markDown_setsAllStatusFalse() {
        MonitoredService service = new MonitoredService("http://localhost:8080");
        service.updateInfo(new InfoEndpointResponse("app", "desc", "1.0"));
        service.updateHealth(new HealthEndpointResponse("UP"));

        service.markDown();

        assertFalse(service.isInfoStatus());
        assertFalse(service.isHealthStatus());
        assertEquals("DOWN", service.getHealthResponse().status());
    }
}
