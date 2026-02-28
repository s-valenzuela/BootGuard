package se.valenzuela.monitoring.core.model;

import org.junit.jupiter.api.Test;
import se.valenzuela.monitoring.core.client.InfoEndpointResponse;

import static org.junit.jupiter.api.Assertions.*;

class MonitoredServiceTest {

    @Test
    void constructor_setsDefaults() {
        MonitoredService service = new MonitoredService("http://localhost:8080");

        assertEquals("http://localhost:8080", service.getUrl());
        assertNull(service.getName());
        assertNull(service.getVersion());
        assertNotNull(service.getLastUpdated());
        assertEquals(MonitoredService.DEFAULT_INFO_ENDPOINT, service.getInfoEndpoint());
        assertEquals(MonitoredService.DEFAULT_HEALTH_ENDPOINT, service.getHealthEndpoint());
    }

    @Test
    void noArgConstructor_existsForJpa() {
        assertDoesNotThrow(() -> {
            var constructor = MonitoredService.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            MonitoredService service = constructor.newInstance();
            assertNull(service.getUrl());
        });
    }

    @Test
    void updateInfo_setsFields() {
        MonitoredService service = new MonitoredService("http://localhost:8080");
        var info = new InfoEndpointResponse("my-app", "My App", "2.0.0");

        service.updateInfo(info);

        assertEquals("my-app", service.getName());
        assertEquals("2.0.0", service.getVersion());
    }

    @Test
    void effectiveInterval_defaultsTo30WhenNoOverrides() {
        MonitoredService service = new MonitoredService("http://localhost:8080");

        assertEquals(30, service.getEffectiveHealthCheckIntervalSeconds());
    }

    @Test
    void effectiveInterval_serviceOverrideWins() {
        MonitoredService service = new MonitoredService("http://localhost:8080");
        service.setHealthCheckIntervalSeconds(15);

        Environment env = new Environment("prod", "#000", 1);
        env.setHealthCheckIntervalSeconds(60);
        service.getEnvironments().add(env);

        assertEquals(15, service.getEffectiveHealthCheckIntervalSeconds());
    }

    @Test
    void effectiveInterval_usesMinOfEnvironments() {
        MonitoredService service = new MonitoredService("http://localhost:8080");

        Environment prod = new Environment("prod", "#000", 1);
        prod.setHealthCheckIntervalSeconds(10);
        Environment dev = new Environment("dev", "#fff", 2);
        dev.setHealthCheckIntervalSeconds(60);
        service.getEnvironments().add(prod);
        service.getEnvironments().add(dev);

        assertEquals(10, service.getEffectiveHealthCheckIntervalSeconds());
    }

    @Test
    void effectiveInterval_ignoresNullEnvironmentIntervals() {
        MonitoredService service = new MonitoredService("http://localhost:8080");

        Environment prod = new Environment("prod", "#000", 1);
        prod.setHealthCheckIntervalSeconds(20);
        Environment dev = new Environment("dev", "#fff", 2);
        // dev has null interval
        service.getEnvironments().add(prod);
        service.getEnvironments().add(dev);

        assertEquals(20, service.getEffectiveHealthCheckIntervalSeconds());
    }

    @Test
    void effectiveInterval_allEnvironmentsNull_defaultsTo30() {
        MonitoredService service = new MonitoredService("http://localhost:8080");

        Environment prod = new Environment("prod", "#000", 1);
        Environment dev = new Environment("dev", "#fff", 2);
        service.getEnvironments().add(prod);
        service.getEnvironments().add(dev);

        assertEquals(30, service.getEffectiveHealthCheckIntervalSeconds());
    }
}
